package com.karyaplatform.karya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskInfo
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskStatus
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.utils.Result
import com.karyaplatform.karya.data.model.karya.enums.ScenarioType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
  private val taskRepository: TaskRepository,
  private val assignmentRepository: AssignmentRepository,
  private val authManager: AuthManager,
) : ViewModel() {

  private var taskInfoList = listOf<TaskInfo>()
  private val taskInfoComparator =
    compareByDescending<TaskInfo> { taskInfo -> taskInfo.taskStatus.assignedMicrotasks }.thenByDescending { taskInfo ->
      taskInfo.taskStatus.skippedMicrotasks
    }.thenBy { taskInfo -> taskInfo.taskID }

  private val _dashboardUiState: MutableStateFlow<DashboardUiState> =
    MutableStateFlow(DashboardUiState.Success(DashboardStateSuccess(emptyList(), 0.0f)))
  val dashboardUiState = _dashboardUiState.asStateFlow()

  private val _workerAccessCode: MutableStateFlow<String> = MutableStateFlow("")
  val workerAccessCode = _workerAccessCode.asStateFlow()

  private val _progress: MutableStateFlow<Int> =
    MutableStateFlow(0)
  val progress = _progress.asStateFlow()

  suspend fun refreshList() {
    val worker = authManager.getLoggedInWorker()
    val tempList = mutableListOf<TaskInfo>()
    taskInfoList.forEach { taskInfo ->
      val taskStatus = fetchTaskStatus(taskInfo.taskID)
      val speechReport = if (taskInfo.scenarioName == ScenarioType.SPEECH_DATA) {
        assignmentRepository.getSpeechReportSummary(worker.id, taskInfo.taskID)
      } else {
        null
      }
      tempList.add(
        TaskInfo(
          taskInfo.taskID,
          taskInfo.taskName,
          taskInfo.taskInstruction,
          taskInfo.scenarioName,
          taskStatus,
          taskInfo.isGradeCard,
          speechReport
        )
      )
    }
    taskInfoList = tempList.sortedWith(taskInfoComparator)

    val totalCreditsEarned = assignmentRepository.getTotalCreditsEarned(worker.id) ?: 0.0f
    _dashboardUiState.value =
      DashboardUiState.Success(DashboardStateSuccess(taskInfoList, totalCreditsEarned))
  }

  /**
   * Returns a hot flow connected to the DB
   * @return [Flow] of list of [TaskRecord] wrapper in a [Result]
   */
  @Suppress("USELESS_CAST")
  fun getAllTasks() {
    viewModelScope.launch {
      val worker = authManager.getLoggedInWorker()
      _workerAccessCode.value = worker.accessCode

      taskRepository
        .getAllTasksFlow()
        .flowOn(Dispatchers.IO)
        .onEach { taskList ->
          val tempList = mutableListOf<TaskInfo>()
          taskList.forEach { taskRecord ->
            val taskInstruction = try {
              taskRecord.params.asJsonObject.get("instruction").asString
            } catch(e: Exception) {
              null
            }
            val taskStatus = fetchTaskStatus(taskRecord.id)
            val speechReport = if (taskRecord.scenario_name == ScenarioType.SPEECH_DATA) {
              assignmentRepository.getSpeechReportSummary(worker.id, taskRecord.id)
            } else {
              null
            }
            tempList.add(
              TaskInfo(
                taskRecord.id,
                taskRecord.display_name,
                taskInstruction,
                taskRecord.scenario_name,
                taskStatus,
                false,
                speechReport
              )
            )
          }
          taskInfoList = tempList

          val totalCreditsEarned = assignmentRepository.getTotalCreditsEarned(worker.id) ?: 0.0f
          val success =
            DashboardUiState.Success(
              DashboardStateSuccess(taskInfoList.sortedWith(taskInfoComparator), totalCreditsEarned)
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

  fun updateTaskStatus(taskId: String) {
    viewModelScope.launch {
      val worker = authManager.getLoggedInWorker()

      val taskStatus = fetchTaskStatus(taskId)

      val updatedList =
        taskInfoList.map { taskInfo ->
          if (taskInfo.taskID == taskId) {
            taskInfo.copy(taskStatus = taskStatus)
          } else {
            taskInfo
          }
        }

      taskInfoList = updatedList
      val totalCreditsEarned = assignmentRepository.getTotalCreditsEarned(worker.id)
      _dashboardUiState.value =
        DashboardUiState.Success(DashboardStateSuccess(taskInfoList, totalCreditsEarned))
    }
  }

  private suspend fun fetchTaskStatus(taskId: String): TaskStatus {
    return taskRepository.getTaskStatus(taskId)
  }

  fun setProgress(i: Int) {
    _progress.value = i
  }

}
