package com.karyaplatform.karya.data.remote.response

import com.google.gson.annotations.SerializedName

data class WorkerBalanceResponse(
  @SerializedName("total_spent") val totalSpent: Float,
  @SerializedName("worker_balance") val workerBalance: Float
)
