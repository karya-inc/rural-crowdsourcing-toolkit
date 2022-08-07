package com.karyaplatform.karya.injection

import com.karyaplatform.karya.data.local.daos.KaryaFileDao
import com.karyaplatform.karya.data.local.daos.MicroTaskDao
import com.karyaplatform.karya.data.local.daos.WorkerDao
import com.karyaplatform.karya.data.local.daosExtra.MicrotaskDaoExtra
import com.karyaplatform.karya.data.repo.*
import com.karyaplatform.karya.data.service.LanguageAPI
import com.karyaplatform.karya.data.service.WorkerAPI
import com.karyaplatform.karya.data.local.daos.PaymentAccountDao
import com.karyaplatform.karya.data.service.PaymentAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {

  @Provides
  @Singleton
  fun provideLanguageRepository(languageAPI: LanguageAPI): LanguageRepository {
    return LanguageRepository(languageAPI)
  }

  @Provides
  @Singleton
  fun provideMicroTaskRepository(
    microTaskDao: MicroTaskDao,
    microtaskDaoExtra: MicrotaskDaoExtra
  ): MicroTaskRepository {
    return MicroTaskRepository(microTaskDao, microtaskDaoExtra)
  }

  @Provides
  @Singleton
  fun provideWorkerRepository(workerAPI: WorkerAPI, workerDao: WorkerDao): WorkerRepository {
    return WorkerRepository(workerAPI, workerDao)
  }

  @Provides
  @Singleton
  fun provideKaryaFileRepository(karyaFileDao: KaryaFileDao): KaryaFileRepository {
    return KaryaFileRepository(karyaFileDao)
  }

  @Provides
  @Singleton
  fun provideAuthRepository(workerDao: WorkerDao): AuthRepository {
    return AuthRepository(workerDao)
  }

  @Provides
  @Singleton
  fun providesPaymentRepository(paymentAPI: PaymentAPI, paymentAccountDao: PaymentAccountDao): PaymentRepository {
    return PaymentRepository(paymentAPI, paymentAccountDao)
  }
}
