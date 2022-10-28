package com.karyaplatform.karya.data.manager

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.DelegatingWorkerFactory
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.KaryaFileRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.data.repo.PaymentRepository
import com.karyaplatform.karya.data.repo.WorkerRepository
import javax.inject.Inject

class SyncDelegatingWorkerFactory
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  karyaFileRepository: KaryaFileRepository,
  microTaskRepository: MicroTaskRepository,
  paymentRepository: PaymentRepository,
  workerRepository: WorkerRepository,
  @FilesDir private val fileDirPath: String,
  authManager: AuthManager,
) : DelegatingWorkerFactory() {
  init {
    addFactory(WorkerFactory(
      assignmentRepository,
      karyaFileRepository,
      microTaskRepository,
      paymentRepository,
      workerRepository,
      fileDirPath,
      authManager))
    // Add here other factories that you may need in your application
  }
}
