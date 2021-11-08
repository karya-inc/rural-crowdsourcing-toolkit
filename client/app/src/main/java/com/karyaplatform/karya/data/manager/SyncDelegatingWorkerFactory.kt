package com.karyaplatform.karya.data.manager

import androidx.work.DelegatingWorkerFactory
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.KaryaFileRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import javax.inject.Inject

class SyncDelegatingWorkerFactory @Inject
constructor(
  assignmentRepository: AssignmentRepository,
  karyaFileRepository: KaryaFileRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir private val fileDirPath: String,
  authManager: AuthManager,
) : DelegatingWorkerFactory() {
  init {
    addFactory(
      WorkerFactory(
        assignmentRepository,
        karyaFileRepository,
        microTaskRepository,
        fileDirPath,
        authManager
      )
    )
    // Add here other factories that you may need in your application
  }
}
