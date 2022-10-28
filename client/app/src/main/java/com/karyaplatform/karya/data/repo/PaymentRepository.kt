package com.karyaplatform.karya.data.repo

import com.karyaplatform.karya.data.local.daos.PaymentAccountDao
import com.karyaplatform.karya.data.model.karya.PaymentAccountRecord
import com.karyaplatform.karya.data.model.karya.enums.AccountRecordStatus
import com.karyaplatform.karya.data.remote.request.PaymentAccountRequest
import com.karyaplatform.karya.data.remote.request.PaymentVerifyRequest
import com.karyaplatform.karya.data.remote.response.PaymentInfoResponse
import com.karyaplatform.karya.data.service.PaymentAPI
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import com.karyaplatform.karya.data.model.karya.modelsExtra.EarningStatus
import com.karyaplatform.karya.utils.PreferenceKeys
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PaymentRepository
@Inject
constructor(
  private val paymentAPI: PaymentAPI,
  private val paymentAccountDao: PaymentAccountDao,
  private val datastore: DataStore<Preferences>,
) {

  suspend fun updatePaymentRecord(workerId: String, paymentInfoResponse: PaymentInfoResponse) =
    withContext(Dispatchers.IO) {

      val ifsc = try {
        paymentInfoResponse.meta!!.account.ifsc!!
      } catch (e: Exception) {
        ""
      }

      val paymentAccountRecord =
        PaymentAccountRecord(
          workerId = workerId,
          accountRecordId = paymentInfoResponse.id ?: "",
          accountType = paymentInfoResponse.accountType,
          failure_reason = "",
          status = AccountRecordStatus.valueOf(paymentInfoResponse.status),
          ifsc = ifsc,
          name = paymentInfoResponse.meta!!.name
        )

      paymentAccountDao.insertForUpsert(paymentAccountRecord)
    }

  suspend fun getAccountRecordId(workerId: String) =
    withContext(Dispatchers.IO) {
      return@withContext paymentAccountDao.getAccountRecordIdForWorkerId(workerId)
    }

  fun addAccount(idToken: String, paymentAccountRequest: PaymentAccountRequest) = flow {
    if (idToken.isEmpty()) {
      error("Either Access Code or ID Token is required")
    }

    val response = paymentAPI.addAccount(idToken, paymentAccountRequest)
    val accountResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account: ${response.errorBody()?.byteString()}")
    }

    if (accountResponse != null) {
      emit(accountResponse)
    } else {
      error("Failed to add account")
    }
  }

  fun getCurrentAccount(idToken: String) = flow {
    if (idToken.isEmpty()) {
      error("Either Access Code or ID Token is required")
    }

    val response = paymentAPI.getCurrentAccountStatus(idToken)
    val accountResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account: ${response.errorBody()?.byteString()}")
    }

    if (accountResponse != null) {
      emit(accountResponse)
    } else {
      error("Failed to add account")
    }
  }

  fun verifyAccount(idToken: String, id: String, confirm: Boolean) = flow {
    if (idToken.isEmpty()) {
      error("Either Access Code or ID Token is required")
    }

    if (id.isEmpty()) {
      error("Worker account id is required")
    }

    val request = PaymentVerifyRequest(confirm)
    val response = paymentAPI.verifyAccount(idToken, id, request)
    val verifyResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account")
    }

    if (verifyResponse != null) {
      emit(verifyResponse)
    } else {
      error("Failed to add account")
    }
  }

  fun getTransactions(idToken: String, workerId: String) = flow {
    if (idToken.isEmpty()) {
      error("Either Access Code or ID Token is required")
    }

    if (workerId.isEmpty()) {
      error("Worker account id is required")
    }

    val response = paymentAPI.getTransactions(idToken, INITIAL_TIME)
    val transactionsResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account")
    }

    if (transactionsResponse != null) {
      emit(transactionsResponse)
    } else {
      error("Failed to add account")
    }
  }

  suspend fun refreshWorkerEarnings(idToken: String) = flow {
    if (idToken.isEmpty()) {
      error("Either Access Code or ID Token is required")
    }

    val response = paymentAPI.getWorkerEarnings(idToken)
    val earningsResponse = response.body()

    if (!response.isSuccessful) {
      error("Failed to add account")
    }

    if (earningsResponse != null) {
      val weekEarned = floatPreferencesKey(PreferenceKeys.WEEK_EARNED)
      val totalPaid = floatPreferencesKey(PreferenceKeys.TOTAL_PAID)
      val totalEarned = floatPreferencesKey(PreferenceKeys.TOTAL_EARNED)
      datastore.edit { prefs -> prefs[weekEarned] = earningsResponse.weekEarned }
      datastore.edit { prefs -> prefs[totalPaid] = earningsResponse.totalPaid }
      datastore.edit { prefs -> prefs[totalEarned] = earningsResponse.totalEarned }
      with (earningsResponse) {
        emit(EarningStatus(this.weekEarned, this.totalEarned, this.totalPaid))
      }
    } else {
      error("Failed to add account")
    }
  }

  suspend fun getWorkerEarnings(): EarningStatus {
    val weekEarnedKey = floatPreferencesKey(PreferenceKeys.WEEK_EARNED)
    val totalEarnedKey = floatPreferencesKey(PreferenceKeys.TOTAL_EARNED)
    val totalPaidKey = floatPreferencesKey(PreferenceKeys.TOTAL_PAID)
    val data = datastore.data.first()
    val weekEarned: Float = data[weekEarnedKey] ?: 0f
    val totalEarned: Float = data[totalEarnedKey] ?: 0f
    val totalPaid: Float = data[totalPaidKey] ?: 0f

    return EarningStatus(weekEarned, totalEarned, totalPaid)
  }

  suspend fun getPaymentRecordStatus(workerId: String) =
    withContext(Dispatchers.IO) {
      val count = paymentAccountDao.getPaymentRecordCount()
      if (count == 0) return@withContext AccountRecordStatus.UNINITIALISED
      val status = paymentAccountDao.getStatusForWorkerId(workerId)
      println(status)
      return@withContext status
    }

  companion object {
    private const val INITIAL_TIME = "1970-01-01T00:00:00Z"
  }
}
