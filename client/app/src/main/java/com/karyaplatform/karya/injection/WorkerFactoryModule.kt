package com.karyaplatform.karya.injection

import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.manager.SyncDelegatingWorkerFactory
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.KaryaFileRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.karyaplatform.karya.data.repo.PaymentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class WorkerFactoryModule {

  @Provides
  @Singleton
  fun providesNgWorkerFactory(
    assignmentRepository: AssignmentRepository,
    karyaFileRepository: KaryaFileRepository,
    microTaskRepository: MicroTaskRepository,
    paymentRepository: PaymentRepository,
    datastore: DataStore<Preferences>,
    @FilesDir fileDirPath: String,
    authManager: AuthManager,
  ): SyncDelegatingWorkerFactory {
    val workerFactory =
      SyncDelegatingWorkerFactory(
        assignmentRepository,
        karyaFileRepository,
        microTaskRepository,
        paymentRepository,
        datastore,
        fileDirPath,
        authManager
      )

    return workerFactory
  }
}
