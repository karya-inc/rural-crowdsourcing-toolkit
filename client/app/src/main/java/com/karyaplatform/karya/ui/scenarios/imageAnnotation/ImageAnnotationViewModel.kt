package com.karyaplatform.karya.ui.scenarios.imageAnnotation

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jsibbold.zoomage.dataClass.Polygon
import com.jsibbold.zoomage.enums.CropObjectType
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import com.karyaplatform.karya.data.model.karya.enums.MicrotaskAssignmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageAnnotationViewModel
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  taskRepository: TaskRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir fileDirPath: String,
  authManager: AuthManager,
  datastore: DataStore<Preferences>
) : BaseMTRendererViewModel(
  assignmentRepository,
  taskRepository,
  microTaskRepository,
  fileDirPath,
  authManager,
  datastore
) {

  // Image to be shown
  private val _imageFilePath: MutableStateFlow<String> = MutableStateFlow("")
  val imageFilePath = _imageFilePath.asStateFlow()

  // Labelled Box coordinates
  private val _rectangleCoors: MutableStateFlow<HashMap<String, RectF>> = MutableStateFlow(HashMap())
  val rectangleCoors = _rectangleCoors.asStateFlow()

  private val _updateRectangles = MutableStateFlow(0)
  val updateRectangles = _updateRectangles.asStateFlow()

  // Labelled Polygon coordinates
  private val _polygonCoors: MutableStateFlow<HashMap<String, Polygon>> = MutableStateFlow(HashMap())
  val polygonCoors = _polygonCoors.asStateFlow()

  var imagePreDrawn = false
  var inputAnnotationsSet = false

  // Annotation type
  var annotationType = CropObjectType.RECTANGLE;
  // Number of sides
  var numberOfSides = 4;
  // Do we need to remember state of annotation
  var rememberAnnotationState = false;
  // Current Image Matrix
  var imageMatrix: Matrix? = null

  // Trigger Spotlight
  private val _playRecordPromptTrigger: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val playRecordPromptTrigger = _playRecordPromptTrigger.asStateFlow()
  /**
   * Setup image annotation microtask
   */
  override fun setupMicrotask() {
    // Get and set the image file
    _imageFilePath.value = try {
      val imageFileName =
        currentMicroTask.input.asJsonObject.getAsJsonObject("files").get("image").asString
      microtaskInputContainer.getMicrotaskInputFilePath(currentMicroTask.id, imageFileName)
    } catch (e: Exception) {
      ""
    }
    // Get image annotation type
    val annotationTypeString = try {
      currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("annotationType").asString
    } catch (e: Exception) {
      "RECTANGLE"
    }

    annotationType = if (annotationTypeString == "POLYGON") CropObjectType.POLYGON
      else CropObjectType.RECTANGLE

    // Get number of sides
    numberOfSides = try {
      currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("numberOfSides").asInt
    } catch (e: Exception) {
      // Since default shape is rectangle
      4
    }

    // Remember state?
    rememberAnnotationState = try {
      task.params.asJsonObject.get("rememberAnnotationState").asBoolean
    } catch (e: Exception) {
      false
    }
    rememberAnnotationState = true
  }

  override fun onFirstTimeVisit() {
    onAssistantClick()
  }

  private fun onAssistantClick() {
    playRecordPrompt()
  }

  private fun playRecordPrompt() {
    _playRecordPromptTrigger.value = true
  }

  fun setInputAnnotations() {
    if (!isCurrentMicrotaskInitialized()) return

    if (!currentMicroTask.input.asJsonObject.getAsJsonObject("data").has("annotations")) return

    val annotations = try {
      currentMicroTask.input.asJsonObject.getAsJsonObject("data").getAsJsonObject("annotations")
    } catch (e: Exception) {
      JsonObject()
    }

    if (annotationType == CropObjectType.RECTANGLE) {
      setInputRectangleAnnotations(annotations)
    } else {
      setInputPolygonAnnotations(annotations)
    }
  }

  private fun setInputPolygonAnnotations(annotations: JsonObject) {}

  private fun setInputRectangleAnnotations(annotations: JsonObject) {
    // HACK: Assume only one label and only one annotation in that label
    val label = annotations.keySet().elementAt(0)
    val rcs = annotations.getAsJsonArray(label).get(0).asJsonArray
    val rectangle = RectF(rcs.get(0).asFloat, rcs.get(1).asFloat, rcs.get(2).asFloat, rcs.get(3).asFloat)
    val rectangleMap = hashMapOf(Pair(label, rectangle))
    setRectangleCoors(rectangleMap)
    _updateRectangles.value = _updateRectangles.value + 1
  }

  /**
   * render the output data on screen
   * TODO: Generalise this method as it only works for single polygon crop object
   */
  fun renderOutputData() {
    if (!isCurrentAssignmentInitialized() || currentAssignment.status != MicrotaskAssignmentStatus.COMPLETED) {
      return
    }
    val outputData = currentAssignment.output.asJsonObject.getAsJsonObject("data")
    val annotations = outputData.getAsJsonObject("annotations")
    // taking first label for now TODO: Generalise for all labels
    val label = annotations.keySet().elementAt(0)
    // Get coordinates with respect to a label for the first crop object
    val coorsJsonArray = annotations.getAsJsonArray(label).get(0).asJsonArray
    val coors = Array<PointF>(coorsJsonArray.size()) { PointF(0F, 0F) }

    for (i in coors.indices) {
      val ele = coorsJsonArray.get(i)
      val x = ele.asJsonArray.get(0).asFloat
      val y = ele.asJsonArray.get(1).asFloat
      coors[i] = PointF(x, y)
    }
    val polygon = Polygon(coors)
    val polygonMap = hashMapOf<String, Polygon>(Pair(label, polygon))
    setPolygonCoors(polygonMap)
  }

  /**
   * Set box Coors
   */
  fun setRectangleCoors(boxCoors: HashMap<String, RectF>) {
    _rectangleCoors.value = boxCoors
  }

  /**
   * Set polygon Coors
   */
  fun setPolygonCoors(polygonCoors: HashMap<String, Polygon>) {
    _polygonCoors.value = polygonCoors
  }

  /**
   * Handle next click
   */
  fun handleNextCLick() {
    var annotationsH = HashMap<String, JsonArray>();
    if (annotationType == CropObjectType.RECTANGLE) {
      annotationsH = getRectangleAnnotation()
    } else {
      annotationsH = getPolygonAnnotation()
    }

    // Convert map to json
    val annotations = JsonObject()
    annotationsH.keys.forEach {
      annotations.add(it, annotationsH[it])
    }

    imagePreDrawn = false
    inputAnnotationsSet = false

    outputData.add("annotations", annotations)
    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }

  /**
   * Get Rectangle annotations
   */
  fun getRectangleAnnotation(): HashMap<String, JsonArray> {
    val annotationsH = HashMap<String, JsonArray>()
    rectangleCoors.value.keys.forEach {
      val coordinates = JsonArray()
      val rectF = rectangleCoors.value[it]!!
      // Add coordinates in the JSON array
      coordinates.add(rectF.left)
      coordinates.add(rectF.top)
      coordinates.add(rectF.right)
      coordinates.add(rectF.bottom)
      // Make a split on "_" and take the first element as the key
      val key = it.split("_")[0]
      if (annotationsH.containsKey(key)) {
        annotationsH[key]?.add(coordinates)
      } else {
        val list = JsonArray()
        list.add(coordinates)
        annotationsH[key] = list
      }
    }
    return annotationsH;
  }

  /**
   * Get Polygon Annotation
   */
  private fun getPolygonAnnotation(): HashMap<String, JsonArray> {
    val annotationsH = HashMap<String, JsonArray>()
    polygonCoors.value.keys.forEach {
      val coordinates = JsonArray()
      val polygon = polygonCoors.value[it]!!
      // Add coordinates in the JSON array
      for (point in polygon.points) {
        val coor = JsonArray()
        coor.add(point.x)
        coor.add(point.y)
        coordinates.add(coor)
      }
      // Make a split on "_" and take the first element as the key
      val key = it.split("_")[0]
      if (annotationsH.containsKey(key)) {
        annotationsH[key]?.add(coordinates)
      } else {
        val list = JsonArray()
        list.add(coordinates)
        annotationsH[key] = list
      }
    }
    return annotationsH;
  }

}
