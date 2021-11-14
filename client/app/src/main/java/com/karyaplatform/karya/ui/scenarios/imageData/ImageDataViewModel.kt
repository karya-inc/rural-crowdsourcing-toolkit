package com.karyaplatform.karya.ui.scenarios.imageData

import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDataViewModel
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  taskRepository: TaskRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir fileDirPath: String,
  authManager: AuthManager,
) : BaseMTRendererViewModel(
  assignmentRepository,
  taskRepository,
  microTaskRepository,
  fileDirPath,
  authManager
) {
  private var imageId = 0

  private val _newImageCount: MutableStateFlow<Pair<Int, Int>> = MutableStateFlow(Pair(0, 0))
  val newImageCount = _newImageCount.asStateFlow()

  /**
   * Setup image data collection microtask
   */
  override fun setupMicrotask() {
    // Get number of images to be captured
    val numImages = try {
      currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("count").asInt
    } catch (e: Exception) {
      0
    }

    // Set new image count
    imageId ++
    _newImageCount.value = Pair(imageId, numImages + 1)
  }

  /**
   * Get output file params for image at an index
   */
  private fun outputFileParams(index: Int): Pair<String, String> {
    return Pair("p$index", "jpg")
  }

  /**
   * Output file name for a given index
   */
  fun outputFilePath(index: Int): String {
    val assignmentId = microtaskAssignmentIDs[currentAssignmentIndex]
    return assignmentOutputContainer.getAssignmentOutputFilePath(assignmentId, outputFileParams(index))
  }

  /**
   * Complete microtask. Add all output files.
   */
  fun completeDataCollection(imageState: MutableList<Boolean>) {
    imageState.forEachIndexed { index, present ->
      if (present) {
        addOutputFile("p$index", outputFileParams(index))
      }
      // Need to do something here
    }

    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }
}
