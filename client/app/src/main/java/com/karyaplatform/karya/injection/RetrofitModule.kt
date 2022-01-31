package com.karyaplatform.karya.injection

import android.content.Context
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.manager.BaseUrlManager
import com.karyaplatform.karya.data.remote.interceptors.HostSelectionInterceptor
import com.karyaplatform.karya.data.remote.interceptors.IdTokenRenewInterceptor
import com.karyaplatform.karya.data.remote.interceptors.VersionInterceptor
import com.karyaplatform.karya.data.repo.AuthRepository
import com.karyaplatform.karya.data.service.KaryaFileAPI
import com.karyaplatform.karya.data.service.LanguageAPI
import com.karyaplatform.karya.data.service.MicroTaskAssignmentAPI
import com.karyaplatform.karya.data.service.WorkerAPI
import com.karyaplatform.karya.injection.qualifier.BaseUrl
import com.karyaplatform.karya.injection.qualifier.KaryaOkHttpClient
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RetrofitModule {

  @Provides
  @Reusable
  fun provideGsonConverterFactory(): GsonConverterFactory {
    return GsonConverterFactory.create()
  }

  @Provides
  @Reusable
  @BaseUrl
  fun provideBaseUrl(): String {
    return "http://__url__"
  }

  @Provides
  @Singleton
  fun provideBaseUrlManager(@ApplicationContext context: Context): BaseUrlManager {
    return BaseUrlManager(context)
  }

  @Provides
  @Reusable
  fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor()
      .setLevel(HttpLoggingInterceptor.Level.BODY)
  }

  @Provides
  @Reusable
  fun provideIdTokenRenewInterceptor(
    authRepository: AuthRepository,
    authManager: AuthManager,
    @BaseUrl baseUrl: String
  ): IdTokenRenewInterceptor {
    return IdTokenRenewInterceptor(authRepository, authManager, baseUrl)
  }

  @Provides
  @Reusable
  fun provideHostSelectionInterceptor(baseUrlManager: BaseUrlManager): HostSelectionInterceptor {
    return HostSelectionInterceptor(baseUrlManager)
  }

  @Provides
  @Reusable
  fun provideVersionInterceptor(): VersionInterceptor {
    return VersionInterceptor()
  }

  @KaryaOkHttpClient
  @Provides
  @Reusable
  fun provideOkHttp(
    idTokenRenewInterceptor: IdTokenRenewInterceptor,
    versionInterceptor: VersionInterceptor,
    hostSelectionInterceptor: HostSelectionInterceptor
  ): OkHttpClient {
    return OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .addInterceptor(idTokenRenewInterceptor)
      .addInterceptor(versionInterceptor)
      .addInterceptor(hostSelectionInterceptor)
      .build()
  }

  @Provides
  @Reusable
  fun provideRetrofitInstance(
    @BaseUrl baseUrl: String,
    converterFactory: GsonConverterFactory,
    @KaryaOkHttpClient okHttpClient: OkHttpClient
  ): Retrofit {
    return Retrofit.Builder().client(okHttpClient).baseUrl(baseUrl)
      .addConverterFactory(converterFactory).build()
  }

  @Provides
  @Reusable
  fun provideLanguageAPI(retrofit: Retrofit): LanguageAPI {
    return retrofit.create(LanguageAPI::class.java)
  }

  @Provides
  @Reusable
  fun provideMicroTaskAPI(retrofit: Retrofit): MicroTaskAssignmentAPI {
    return retrofit.create(MicroTaskAssignmentAPI::class.java)
  }

  @Provides
  @Reusable
  fun provideWorkerAPI(retrofit: Retrofit): WorkerAPI {
    return retrofit.create(WorkerAPI::class.java)
  }

  @Provides
  @Reusable
  fun provideKaryaFileAPIService(retrofit: Retrofit): KaryaFileAPI {
    return retrofit.create(KaryaFileAPI::class.java)
  }
}
