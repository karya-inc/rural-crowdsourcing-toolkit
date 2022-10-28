package com.karyaplatform.karya.data.service

import com.karyaplatform.karya.data.remote.request.PaymentAccountRequest
import com.karyaplatform.karya.data.remote.request.PaymentVerifyRequest
import com.karyaplatform.karya.data.remote.response.PaymentInfoResponse
import com.karyaplatform.karya.data.remote.response.PaymentTransactionResponse
import com.karyaplatform.karya.data.remote.response.WorkerBalanceResponse
import com.karyaplatform.karya.data.remote.response.WorkerEarningsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentAPI {
  @POST("/payments/accounts")
  suspend fun addAccount(
    @Header("karya-id-token") idToken: String,
    @Body body: PaymentAccountRequest
  ): Response<PaymentInfoResponse>

  @PUT("/payments/accounts/{id}/verify")
  suspend fun verifyAccount(
    @Header("karya-id-token") idToken: String,
    @Path("id") accountRecordId: String,
    @Body paymentVerifyRequest: PaymentVerifyRequest,
  ): Response<PaymentInfoResponse>

  @GET("/payments/transaction")
  suspend fun getTransactions(
    @Header("karya-id-token") idToken: String,
    @Query("from") timeToken: String,
  ): Response<List<PaymentTransactionResponse>>

  @GET("/payments/accounts/current")
  suspend fun getCurrentAccountStatus(
    @Header("karya-id-token") idToken: String,
  ): Response<PaymentInfoResponse>

  @GET("/payments/worker/earnings")
  suspend fun getWorkerEarnings(
    @Header("karya-id-token") idToken: String,
  ): Response<WorkerEarningsResponse>
}
