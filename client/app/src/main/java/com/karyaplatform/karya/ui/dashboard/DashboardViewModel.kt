package com.karyaplatform.karya.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskInfo
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskStatus
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.utils.Result
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
    compareByDescending<TaskInfo> { taskInfo -> taskInfo.taskStatus.assignedMicrotasks }.thenBy { taskInfo ->
      taskInfo.taskID
    }

  private val _dashboardUiState: MutableStateFlow<DashboardUiState> =
    MutableStateFlow(DashboardUiState.Success(DashboardStateSuccess(emptyList(), 0.0f)))
  val dashboardUiState = _dashboardUiState.asStateFlow()

  private val _progress: MutableStateFlow<Int> =
    MutableStateFlow(0)
  val progress = _progress.asStateFlow()

  suspend fun refreshList() {
    val worker = authManager.getLoggedInWorker()
    val tempList = mutableListOf<TaskInfo>()
    taskInfoList.forEach { taskInfo ->
      val taskStatus = fetchTaskStatus(taskInfo.taskID)
      tempList.add(
        TaskInfo(
          taskInfo.taskID,
          taskInfo.taskName,
          taskInfo.scenarioName,
          taskStatus,
          taskInfo.isGradeCard
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

      taskRepository
        .getAllTasksFlow()
        .flowOn(Dispatchers.IO)
        .onEach { taskList ->
          val tempList = mutableListOf<TaskInfo>()
          taskList.forEach { taskRecord ->
            val taskStatus = fetchTaskStatus(taskRecord.id)
            tempList.add(
              TaskInfo(
                taskRecord.id,
                taskRecord.display_name,
                taskRecord.scenario_name,
                taskStatus,
                false
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
      val totalCreditsEarned = assignmentRepository.getTotalCreditsEarned(worker.id) ?: 0.0f
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
