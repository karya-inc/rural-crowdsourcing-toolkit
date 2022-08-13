package com.karyaplatform.karya.data.repo

import com.karyaplatform.karya.data.local.daos.MicroTaskAssignmentDao
import com.karyaplatform.karya.data.local.daos.TaskDao
import com.karyaplatform.karya.data.model.karya.TaskRecord
import com.karyaplatform.karya.data.model.karya.enums.MicrotaskAssignmentStatus
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepository
@Inject
constructor(
  private val taskDao: TaskDao,
  private val microTaskAssignmentDao: MicroTaskAssignmentDao,
) {
  suspend fun getById(taskId: String): TaskRecord {
    return taskDao.getById(taskId)
  }

  fun getAllTasksFlow(): Flow<List<TaskRecord>> = taskDao.getAllAsFlow()

  suspend fun getTaskStatus(taskId: String): TaskStatus {
    val available = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.ASSIGNED)
    val completed = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.COMPLETED)
    val submitted = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.SUBMITTED)
    val verified = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.VERIFIED)
    val skipped = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.SKIPPED)
    val expired = microTaskAssignmentDao.getCountForTask(taskId, MicrotaskAssignmentStatus.EXPIRED)

    return TaskStatus(available, completed, submitted, verified, skipped, expired)
  }

  suspend fun getTaskSummary(): TaskStatus {
    val available = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.ASSIGNED)
    val completed = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.COMPLETED)
    val submitted = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.SUBMITTED)
    val verified = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.VERIFIED)
    val skipped = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.SKIPPED)
    val expired = microTaskAssignmentDao.getCountByStatus(MicrotaskAssignmentStatus.EXPIRED)

    return TaskStatus(available, completed, submitted, verified, skipped, expired)
  }

}
