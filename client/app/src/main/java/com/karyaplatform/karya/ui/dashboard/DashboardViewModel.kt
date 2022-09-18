package com.karyaplatform.karya.ui.dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskInfo
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskStatus
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.utils.Result
import com.karyaplatform.karya.utils.DateUtils
import com.karyaplatform.karya.utils.PreferenceKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

private const val WFC_CODE_SEED="93848374"

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
  private val taskRepository: TaskRepository,
  private val assignmentRepository: AssignmentRepository,
  private val authManager: AuthManager,
  private val datastore: DataStore<Preferences>
) : ViewModel() {

  private var taskInfoList = listOf<TaskInfo>()
  private val taskInfoComparator =
    compareByDescending<TaskInfo> { taskInfo -> taskInfo.taskStatus.assignedMicrotasks }
      .thenByDescending { taskInfo -> taskInfo.taskStatus.skippedMicrotasks }
      .thenBy { taskInfo -> taskInfo.taskID }

  private val _dashboardUiState: MutableStateFlow<DashboardUiState> =
    MutableStateFlow(DashboardUiState.Success(DashboardStateSuccess(emptyList())))
  val dashboardUiState = _dashboardUiState.asStateFlow()

  private val _progress: MutableStateFlow<Int> = MutableStateFlow(0)
  val progress = _progress.asStateFlow()

  private val _workerAccessCode: MutableStateFlow<String> = MutableStateFlow("")
  val workerAccessCode = _workerAccessCode.asStateFlow()

  // Work from center user
  private val _workFromCenterUser: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val workFromCenterUser = _workFromCenterUser.asStateFlow()
  private val _userInCenter: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val userInCenter = _userInCenter.asStateFlow()
  var centerAuthExpirationTime: Long = 0

  init {
    viewModelScope.launch {
      val worker = authManager.getLoggedInWorker()
      _workerAccessCode.value = worker.accessCode

      try {
        if (worker.params != null && !worker.params.isJsonNull) {
          val tags = worker.params.asJsonObject.getAsJsonArray("tags")
          for (tag in tags) {
            if (tag.asString == "_wfc_") {
              _workFromCenterUser.value = true
            }
          }
        }
      } catch (e: Exception) {
        _workFromCenterUser.value = false
      }
    }
  }

  fun authorizeWorkFromCenterUser(code: String) {
    val today = DateUtils.getCurrentDate().substring(0,10)
    val message = WFC_CODE_SEED + today + "\n"
    val md5Encoder = MessageDigest.getInstance("MD5")
    md5Encoder.update(message.toByteArray(), 0, message.length)
    val hash = BigInteger(1, md5Encoder.digest()).toString(16).substring(0,6)
    if (code == hash) {
      _userInCenter.value = true
      // TODO: hour offset is hard coded
      centerAuthExpirationTime = Date().time + 2 * 60 * 60 * 1000
    }
  }

  fun revokeWFCAuthorization() {
    _userInCenter.value = false
  }

  fun checkWorkFromCenterUserAuth() {
    val currentTime = Date().time
    if (currentTime > centerAuthExpirationTime) {
      _userInCenter.value = false
    }
  }

  suspend fun refreshList() {
    val worker = authManager.getLoggedInWorker()
    val tempList = mutableListOf<TaskInfo>()

    // Get task report summary
    val taskSummary = assignmentRepository.getTaskReportSummary(worker.id)

    taskInfoList.forEach { taskInfo ->
      val taskId = taskInfo.taskID
      val taskStatus = fetchTaskStatus(taskId)
      val summary = if (taskSummary.containsKey(taskId)) taskSummary[taskId] else null
      tempList.add(
        TaskInfo(
          taskInfo.taskID,
          taskInfo.taskName,
          taskInfo.taskInstruction,
          taskInfo.scenarioName,
          taskStatus,
          taskInfo.isGradeCard,
          summary
        )
      )
    }
    taskInfoList = tempList.sortedWith(taskInfoComparator)

    val success =
      DashboardUiState.Success(
        DashboardStateSuccess(taskInfoList.sortedWith(taskInfoComparator))
      )
    _dashboardUiState.value = success
  }

  /**
   * Returns a hot flow connected to the DB
   * @return [Flow] of list of [TaskRecord] wrapper in a [Result]
   */
  @Suppress("USELESS_CAST")
  fun getAllTasks() {
    viewModelScope.launch {
      val worker = authManager.getLoggedInWorker()

      taskRepository
        .getAllTasksFlow()
        .flowOn(Dispatchers.IO)
        .onEach { taskList ->

          // Get task report summary
          val taskSummary = assignmentRepository.getTaskReportSummary(worker.id)

          val tempList = mutableListOf<TaskInfo>()
          taskList.forEach { taskRecord ->
            val taskInstruction = try {
              taskRecord.params.asJsonObject.get("instruction").asString
            } catch (e: Exception) {
              null
            }
            val taskId = taskRecord.id
            val taskStatus = fetchTaskStatus(taskId)
            val summary = if (taskSummary.containsKey(taskId)) taskSummary[taskId] else null

            tempList.add(
              TaskInfo(
                taskRecord.id,
                taskRecord.display_name,
                taskInstruction,
                taskRecord.scenario_name,
                taskStatus,
                false,
                summary
              )
            )
          }
          taskInfoList = tempList

          val success =
            DashboardUiState.Success(
              DashboardStateSuccess(taskInfoList.sortedWith(taskInfoComparator))
            )
          _dashboardUiState.value = success
        }
        .catch { _dashboardUiState.value = DashboardUiState.Error(it) }
        .collect()
    }
  }

  fun setLoading() {
    _dashboardUiState.value = DashboardUiState.Loading
  }

  private suspend fun fetchTaskStatus(taskId: String): TaskStatus {
    return taskRepository.getTaskStatus(taskId)
  }

  fun setProgress(i: Int) {
    _progress.value = i
  }
}
