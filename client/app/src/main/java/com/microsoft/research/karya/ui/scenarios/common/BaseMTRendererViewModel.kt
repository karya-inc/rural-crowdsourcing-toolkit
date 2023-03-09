package com.microsoft.research.karya.ui.scenarios.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.model.karya.MicroTaskAssignmentRecord
import com.microsoft.research.karya.data.model.karya.MicroTaskRecord
import com.microsoft.research.karya.data.model.karya.TaskRecord
import com.microsoft.research.karya.data.model.karya.enums.MicrotaskAssignmentStatus
import com.microsoft.research.karya.data.repo.AssignmentRepository
import com.microsoft.research.karya.data.repo.MicroTaskRepository
import com.microsoft.research.karya.data.repo.TaskRepository
import com.microsoft.research.karya.utils.DateUtils
import com.microsoft.research.karya.utils.FileUtils
import com.microsoft.research.karya.utils.MicrotaskAssignmentOutput
import com.microsoft.research.karya.utils.MicrotaskInput
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.getBlobPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

abstract class BaseMTRendererViewModel(
  var assignmentRepository: AssignmentRepository,
  var taskRepository: TaskRepository,
  var microTaskRepository: MicroTaskRepository,
  var fileDirPath: String,
  var authManager: AuthManager,
  val datastore: DataStore<Preferences>,
  val includeCompleted: Boolean = true
) : ViewModel() {

  private lateinit var taskId: String
  // Is the fragment visited the first time?
  var firstTimeActivityVisit: Boolean = true
    private set

  // Initialising containers
  val assignmentOutputContainer = MicrotaskAssignmentOutput(fileDirPath)
  val microtaskInputContainer = MicrotaskInput(fileDirPath)

  lateinit var task: TaskRecord
  protected lateinit var microtaskAssignmentIDs: List<String>
  protected var currentAssignmentIndex: Int = 0

  lateinit var currentMicroTask: MicroTaskRecord
  lateinit var currentAssignment: MicroTaskAssignmentRecord

  // Microtask group id -- track this to move back to dashboard on group boundaries
  private var groupId: String? = null

  // Output fields for microtask assignment
  // TODO: Maybe make them a data class?
  protected var outputData: JsonObject = JsonObject()
  protected var outputFiles: JsonObject = JsonObject()
  protected var logs: JsonArray = JsonArray()

  private val _navigateBack: MutableSharedFlow<Boolean> = MutableSharedFlow(1)
  val navigateBack = _navigateBack.asSharedFlow()

  private val _completedAssignments = MutableStateFlow<Int>(0)
  val completedAssignments = _completedAssignments.asSharedFlow()

  private val _totalAssignments = MutableStateFlow<Int>(1)
  val totalAssignments = _totalAssignments.asSharedFlow()

  private val _inputFileDoesNotExist: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val inputFileDoesNotExist = _inputFileDoesNotExist.asSharedFlow()

  private val _outsideTimeBound: MutableStateFlow<Triple<Boolean, String, String>> =
    MutableStateFlow(Triple(false, "", ""))
  val outsideTimeBound = _outsideTimeBound.asStateFlow()

  private val _holidayMessage = MutableStateFlow(Pair(false, R.string.sunday_message))
  val holidayMessage = _holidayMessage.asStateFlow()

  open fun onFirstTimeVisit() {}

  open fun onSubsequentVisit() {}

  protected fun isCurrentAssignmentInitialized(): Boolean {
    return this::currentAssignment.isInitialized
  }

  protected fun navigateBack() {
    viewModelScope.launch { _navigateBack.emit(true) }
  }

  open fun setupViewModel(taskId: String, completed: Int, total: Int) {
    this.taskId = taskId
    _totalAssignments.value = total
    _completedAssignments.value = completed

    // TODO: Shift this to init once we move to viewmodel factory
    runBlocking {
      task = taskRepository.getById(taskId)

      // Determine if we have to include completed assignments
      if (includeCompleted) {
        microtaskAssignmentIDs = assignmentRepository.getIDsForTask(
            task.id,
            arrayListOf(MicrotaskAssignmentStatus.COMPLETED)
          )
      }

      microtaskAssignmentIDs =
        microtaskAssignmentIDs + assignmentRepository.getIDsForTask(
          task.id,
          arrayListOf(MicrotaskAssignmentStatus.ASSIGNED)
        ) // TODO: Generalise the includeCompleted parameter (Can be done when we have viewModel factory)

      // Get Skipped Assignments
      microtaskAssignmentIDs = microtaskAssignmentIDs +
        assignmentRepository.getIDsForTask(
          task.id,
          arrayListOf(MicrotaskAssignmentStatus.SKIPPED)
      )

      if (microtaskAssignmentIDs.isEmpty()) {
        navigateBack()
      }

      // Move to the first incomplete (assigned) microtask or the last microtask
      while (currentAssignmentIndex < microtaskAssignmentIDs.size - 1) {
        val microtaskAssignmentID = microtaskAssignmentIDs[currentAssignmentIndex]
        val microtaskAssignment = assignmentRepository.getAssignmentById(microtaskAssignmentID)
        if (microtaskAssignment.status == MicrotaskAssignmentStatus.ASSIGNED ||
          microtaskAssignment.status == MicrotaskAssignmentStatus.SKIPPED) {
          break
        }
        currentAssignmentIndex++
      }
    }
  }

  /**
   * Setup microtask after updating [currentAssignmentIndex]. Called at the end of [onResume], and
   * navigating to next or previous tasks
   */
  protected abstract fun setupMicrotask()

  /** Set the output for an assignment */
  protected fun setOutput(output: JsonObject) {
    outputData = output
  }

  /** Set output property */

  // TODO: Move logging to another module
  /** Add a string message to the log */
  fun log(message: String) {
    val logObj = JsonObject()
    val currentTime = DateUtils.getCurrentDate()
    logObj.add("ts", Gson().toJsonTree(currentTime))
    logObj.add("message", Gson().toJsonTree(message))
    logs.add(logObj)
  }

  /** Add a Json object to the log */
  protected fun log(obj: JsonObject) {
    val logObj = JsonObject()
    val currentTime = DateUtils.getCurrentDate()
    logObj.addProperty("ts", currentTime)
    logObj.add("message", obj)
    logs.add(logObj)
  }

  /** Add a file to the assignment with the given output */
  protected fun addOutputFile(key: String, params: Pair<String, String>) {
    val assignmentId = microtaskAssignmentIDs[currentAssignmentIndex]
    val fileName = assignmentOutputContainer.getAssignmentFileName(assignmentId, params)

    outputFiles.addProperty(key, fileName)

    // log the output file addition
    val logObj = JsonObject()
    logObj.addProperty("type", "output-file")
    logObj.addProperty("filename", fileName)
    log(logObj)
  }

  /**
   * Mark the current microtask as complete with the [outputData], [outputFiles], and [logs]
   * attached to the current assignment's output field. Delete all scratch files.
   */
  protected suspend fun completeAndSaveCurrentMicrotask() {

    log("marking microtask complete")
    val output = buildOutputJsonObject()
    val logObj = JsonObject()
    logObj.add("logs", logs)

    /** Delete all scratch files */
    deleteAssignmentScratchFiles()

    // If assignment is not already completed, increase completed count by 1
    if (currentAssignment.status == MicrotaskAssignmentStatus.ASSIGNED)
      _completedAssignments.value = _completedAssignments.value + 1

    withContext(Dispatchers.IO) {
      assignmentRepository.markComplete(
        microtaskAssignmentIDs[currentAssignmentIndex],
        output,
        logObj,
        date = DateUtils.getCurrentDate()
      )
    }
  }

  protected suspend fun skipAndSaveCurrentMicrotask() {
    /** Delete all scratch files */
    withContext(Dispatchers.IO) {
      assignmentRepository.markSkip(
        microtaskAssignmentIDs[currentAssignmentIndex],
        date = DateUtils.getCurrentDate()
      )
    }
  }

  /**
   * Mark the assignment as expire and save
   */
  protected suspend fun expireAndSaveCurrentMicrotask() {
    /** Delete all scratch files */
    withContext(Dispatchers.IO) {
      assignmentRepository.markExpire(
        microtaskAssignmentIDs[currentAssignmentIndex],
        date = DateUtils.getCurrentDate()
      )
    }
  }

  private fun deleteAssignmentScratchFiles() {
    val directory = File(getRelativePath("microtask-assignment-scratch"))
    val files = directory.listFiles()
    files?.forEach { if (it.exists()) it.delete() }
  }

  private fun buildOutputJsonObject(): JsonObject {
    val output = JsonObject()
    output.add("data", outputData)
    output.add("files", outputFiles)
    return output
  }

  /** Is there a next microtask (for navigation) */
  protected fun hasNextMicrotask(): Boolean {
    return currentAssignmentIndex < (microtaskAssignmentIDs.size - 1)
  }

  /** Is there a previous microtask (for navigation) */
  protected fun hasPreviousMicrotask(): Boolean {
    return currentAssignmentIndex > 0
  }

  /** Move to next microtask and setup. Returns false if there is no next microtask. Else true. */
  protected fun moveToNextMicrotask() {
    if (hasNextMicrotask()) {
      currentAssignmentIndex++
      getAndSetupMicrotask()
      log("moved to next microtask")
    } else {
      navigateBack()
    }
  }

  /**
   * Move to previous microtask and setup. Returns false if there is no previous microtask. Else
   * true
   */
  protected fun moveToPreviousMicrotask() {
    if (hasPreviousMicrotask()) {
      currentAssignmentIndex--
      getAndSetupMicrotask()
      log("moved to previous microtask")
    } else {
      navigateBack()
    }
  }

  /** Get the microtask record for the current assignment and setup the microtask */
  fun getAndSetupMicrotask() {
    viewModelScope.launch {

      val assignmentID = microtaskAssignmentIDs[currentAssignmentIndex]

      // Fetch the assignment and the microtask
      currentAssignment = assignmentRepository.getAssignmentById(assignmentID)
      currentMicroTask = microTaskRepository.getById(currentAssignment.microtask_id)

      // Check if the current assignment is expired
      if (!(currentAssignment.deadline).isNullOrEmpty()
        && (currentAssignment.deadline!!) < DateUtils.getCurrentDate()) {
        // Mark the microtask as expired
        expireAndSaveCurrentMicrotask()
        moveToNextMicrotask()
        return@launch
      }

      // Check if the worker has a start-end constraints
      val worker = authManager.getLoggedInWorker()
      val tcTag = try {
        if (worker.params != null) {
          val tags = worker.params.asJsonObject.getAsJsonArray("tags")
          var tag: String? = null
          for (tagJ in tags) {
            if (tagJ.asString.startsWith("_tc_")) {
              tag = tagJ.asString.substring(4)
            }
          }
          tag
        } else {
          null
        }
      } catch(e: Exception) {
        null
      }

      var taskStartTime = if (tcTag != null) {
        tcTag.split("-")[0]
      } else {
        null
      }

      var taskEndTime = if (tcTag != null) {
        tcTag.split("-")[1]
      } else {
        null
      }

      val taskParams = task.params.asJsonObject

      taskStartTime = if (taskParams.has("startTime")) {
        taskParams.get("startTime").asString.trim()
      } else {
        taskStartTime
      }
      taskEndTime = if (taskParams.has("endTime")) {
        taskParams.get("endTime").asString.trim()
      } else {
        taskEndTime
      }

      taskStartTime = if (taskStartTime == "") null else taskStartTime
      taskEndTime = if (taskEndTime == "") null else taskEndTime

      val currentTime = Calendar.getInstance()

      if (taskStartTime != null && taskEndTime != null) {
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minutes = currentTime.get(Calendar.MINUTE)
        val now = String.format(Locale.US, "%02d:%02d", hour, minutes)

        if (now < taskStartTime || now > taskEndTime) {
          _outsideTimeBound.emit(Triple(true, taskStartTime, taskEndTime))
          return@launch
        }
      }

      // Check for sundays
      val dayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
      if (dayOfWeek == Calendar.SUNDAY) {
        _holidayMessage.emit(Pair(true, R.string.sunday_message))
        return@launch
      }

      // Check for diwali
      val month = currentTime.get(Calendar.MONTH)
      val day = currentTime.get(Calendar.DAY_OF_MONTH)
      if (month == Calendar.OCTOBER && day >= 24 && day <= 26) {
        _holidayMessage.emit(Pair(true, R.string.diwali_message))
        return@launch
      }

      // Check if we are crossing group boundaries
      if (groupId != null && currentMicroTask.group_id != groupId) {
        navigateBack()
        return@launch
      }

      groupId = currentMicroTask.group_id

      /** If microtask has input files, extract them */
      _inputFileDoesNotExist.value = false
      if (currentMicroTask.input_file_id != null) {
        val microtaskTarBallPath = microtaskInputContainer.getBlobPath(currentMicroTask.id)
        val microtaskInputDirectory =
          microtaskInputContainer.getMicrotaskInputDirectory(currentMicroTask.id)

        if (!File(microtaskTarBallPath).exists()) {
          _inputFileDoesNotExist.value = true
        } else {
          FileUtils.extractGZippedTarBallIntoDirectory(
            microtaskTarBallPath,
            microtaskInputDirectory
          )
        }
      }

      if (_inputFileDoesNotExist.value) {
        // Dialog saying input file does not exist?
        navigateBack()
        return@launch
      }

      outputData =
        if (!currentAssignment.output.isJsonNull && currentAssignment.output.asJsonObject.has("data")) {
          currentAssignment.output.asJsonObject.getAsJsonObject("data")
        } else {
          JsonObject()
        }

      outputFiles =
        if (!currentAssignment.output.isJsonNull && currentAssignment.output.asJsonObject.has("files")) {
          currentAssignment.output.asJsonObject.getAsJsonObject("files")
        } else {
          JsonObject()
        }

      logs =
        if (!currentAssignment.logs.isJsonNull && currentAssignment.logs.asJsonObject.has("logs")) {
          currentAssignment.logs.asJsonObject.getAsJsonArray("logs")
        } else {
          JsonArray()
        }

      setupMicrotask()
      log("microtask setup complete")

      // First visit logic
      viewModelScope.launch {
        val scenarioName = "${task.scenario_name.name}"
        val firstRunKey = booleanPreferencesKey(scenarioName)

        val data = datastore.data.first()
        firstTimeActivityVisit = data[firstRunKey] ?: true
        if (firstTimeActivityVisit) {
          onFirstTimeVisit()
        } else {
          onSubsequentVisit()
        }
        datastore.edit { prefs -> prefs[firstRunKey] = false }
        firstTimeActivityVisit = false
      }
    }
  }

  protected fun getRelativePath(s: String): String {
    return "$fileDirPath/$s"
  }

  /**
   * Get the unique file name of the output for current assignment. [params] is a pair of strings: a
   * file identifier and extension. The file name is usually the current assignmentID appended with
   * the identifier. The full file name is unique for a unique [params] pair.
   */
  private fun getAssignmentFileName(params: Pair<String, String>): String {
    val identifier = params.first
    val extension = params.second
    val assignmentId = microtaskAssignmentIDs[currentAssignmentIndex]

    return if (identifier == "") "$assignmentId.$extension" else "$assignmentId-$identifier.$extension"
  }

  /** Get the file path for a scratch file for the current assignment and [params] pair */
  protected fun getAssignmentScratchFilePath(params: Pair<String, String>): String {
    val dir_path = "$fileDirPath/microtask-assignment-scratch"
    val dir = File(dir_path)
    dir.mkdirs()
    val fileName = getAssignmentFileName(params)
    return "$dir_path/$fileName"
  }

  /** Get the file path for an output file for the current assignment and [params] pair */
  // TODO: Move Scratch File functions to Karya Containers
  protected fun getAssignmentScratchFile(params: Pair<String, String>): File {
    val filePath = getAssignmentScratchFilePath(params)
    val file = File(filePath)
    if (file.exists()) file.delete()
    file.createNewFile()
    return file
  }

  /** Reset existing microtask. Useful on activity restart. */
  protected fun resetMicrotask() {
    getAndSetupMicrotask()
  }

  /**
   * Skip the microtask
   */
  fun skipTask() {
    // log the state transition
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "SKIPPED")
    log(message)

    viewModelScope.launch {
      skipAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }
}
