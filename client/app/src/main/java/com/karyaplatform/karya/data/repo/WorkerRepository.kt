package com.karyaplatform.karya.data.repo

import com.karyaplatform.karya.data.exceptions.*
import com.karyaplatform.karya.data.local.daos.WorkerDao
import com.karyaplatform.karya.data.model.karya.WorkerRecord
import com.karyaplatform.karya.data.remote.request.RegisterOrUpdateWorkerRequest
import com.karyaplatform.karya.data.service.WorkerAPI
import com.google.gson.JsonObject
import com.karyaplatform.karya.data.local.daos.LeaderboardDao
import com.karyaplatform.karya.data.model.karya.LeaderboardRecord
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.karyaplatform.karya.data.exceptions.*
import com.karyaplatform.karya.utils.PreferenceKeys
import androidx.datastore.preferences.core.stringPreferencesKey
import com.karyaplatform.karya.data.exceptions.*
import com.karyaplatform.karya.data.model.karya.TaskRecord
import com.karyaplatform.karya.data.model.karya.modelsExtra.EarningStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WorkerRepository @Inject constructor(
  private val workerAPI: WorkerAPI,
  private val workerDao: WorkerDao,
  private val leaderboardDao: LeaderboardDao,
  private val datastore: DataStore<Preferences>
) {

  fun getOTP(
    accessCode: String,
    phoneNumber: String,
  ) = flow {
    val response = workerAPI.generateOTP(accessCode, phoneNumber)
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        401 -> InvalidAccessCodeException()
        403 -> AccessCodeAlreadyUsedException()
        503 -> UnableToGenerateOTPException()
        else -> KaryaException()
      }
    }

    if (workerRecord == null) {
      error("Request failed, response body was null")
    }

    // emit unit at the end to indicate success
    // TODO: think if we should use suspend fun instead of a flow
    emit(Unit)
  }

  fun resendOTP(
    accessCode: String,
    phoneNumber: String,
  ) = flow {
    val response = workerAPI.resendOTP(accessCode, phoneNumber)
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        403 -> AccessCodeAlreadyUsedException()
        401 -> InvalidAccessCodeException()
        503 -> UnableToGenerateOTPException()
        else -> KaryaException()
      }
    }

    if (workerRecord == null) {
      error("Request failed, response body was null")
    }

    emit(Unit)
  }

  fun verifyOTP(
    accessCode: String,
    phoneNumber: String,
    otp: String,
  ) = flow {
    val response = workerAPI.verifyOTP(accessCode, phoneNumber, otp)
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        403 -> AccessCodeAlreadyUsedException()
        401 -> InvalidOTPException()
        else -> KaryaException()
      }
    }

    if (workerRecord != null) {
      emit(workerRecord)
    } else {
      error("Request failed, response body was null")
    }
  }

  fun verifyAccessCode(accessCode: String) = flow {
    val response = workerAPI.getWorkerUsingAccessCode(accessCode)
    val responseBody = response.body()

    if (!response.isSuccessful) {
      error("Request failed, response code: ${response.code()}")
    }

    if (responseBody != null) {
      emit(responseBody)
    } else {
      error("Request failed, response body was null")
    }
  }

  fun getWorkerUsingIdToken(
    idToken: String,
  ) = flow {
    val response = workerAPI.getWorkerUsingIdToken(idToken)
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      error("Request failed, response code: ${response.code()}")
    }

    if (workerRecord != null) {
      emit(workerRecord)
    } else {
      error("Request failed, response body was null")
    }
  }

  fun updateWorker(
    idToken: String,
    registerOrUpdateWorkerRequest: RegisterOrUpdateWorkerRequest,
  ) = flow {
    val response = workerAPI.updateWorker(idToken, registerOrUpdateWorkerRequest, "update")
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        401 -> InvalidAccessCodeException()
        else -> KaryaException()
      }
    }

    if (workerRecord != null) {
      emit(workerRecord)
    } else {
      error("Request failed, response body was null")
    }
  }

  fun updateWorkerProfile(
    idToken: String,
    profile: JsonObject,
  ) = flow {
    val response = workerAPI.updateWorker(idToken, profile)
    val workerRecord = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        401 -> InvalidAccessCodeException()
        else -> KaryaException()
      }
    }

    if (workerRecord != null) {
      emit(workerRecord)
    } else {
      error("Request failed, response body was null")
    }
  }

  suspend fun getAllWorkers() =
    withContext(Dispatchers.IO) {
      return@withContext workerDao.getAll()
    }

  suspend fun getWorkerById(id: String) =
    withContext(Dispatchers.IO) {
      return@withContext workerDao.getById(id)
    }

  suspend fun getWorkerByAccessCode(accessCode: String) =
    withContext(Dispatchers.IO) {
      return@withContext workerDao.getByAccessCode(accessCode)
    }

  suspend fun upsertWorker(worker: WorkerRecord) =
    withContext(Dispatchers.IO) { workerDao.upsert(worker) }

  suspend fun updateLanguage(id: String, lang: String) =
    withContext(Dispatchers.IO) {
      workerDao.updateLanguage(id, lang)
    }

  suspend fun getAllLeaderBoardRecords() = leaderboardDao.getAllLeaderboardRecords()

  suspend fun getXPPoints(worker_id: String): Int? = withContext(Dispatchers.IO) {
    return@withContext leaderboardDao.getXPPoints(worker_id)
  }

  /**
   * The flow updates leaderboard by fetching it from network
   */
  fun updateLeaderboard(
    idToken: String
  ) = flow {
    val response = workerAPI.getLeaderBoard(idToken)
    val leaderboardRecords = response.body()

    if (!response.isSuccessful) {
      throw when (response.code()) {
        401 -> InvalidAccessCodeException()
        else -> KaryaException()
      }
    }

    if (leaderboardRecords != null) {
      saveLeaderboard(leaderboardRecords)
      emit(leaderboardRecords)
    } else {
      error("Request failed, response body was null")
    }
  }

  /**
   * Get Worker's Working Week and Day
   */
  fun getWorkerWorkingWeekAndDay(idToken: String) = flow {
    val response = workerAPI.getWorkerWorkingWeekAndDay(idToken)
    val weekResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account")
    }

    if (weekResponse != null) {
      val regTimeKey = stringPreferencesKey(PreferenceKeys.REG_TIME)
      val weekKey = intPreferencesKey(PreferenceKeys.CURRENT_WEEK)
      val dayKey = intPreferencesKey(PreferenceKeys.CURRENT_DAY)
      datastore.edit { prefs -> prefs[regTimeKey] = weekResponse.regTime.toString() }
      datastore.edit { prefs -> prefs[weekKey] = weekResponse.week }
      datastore.edit { prefs -> prefs[dayKey] = weekResponse.day }
      emit(weekResponse)
    } else {
      error("Failed to add account")
    }
  }

  private suspend fun saveLeaderboard(records: List<LeaderboardRecord>) {
    leaderboardDao.deleteAllRecords()
    leaderboardDao.upsert(records)
  }
}
