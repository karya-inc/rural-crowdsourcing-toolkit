package com.karyaplatform.karya.data.remote.response

import com.google.gson.annotations.SerializedName

data class WorkerWeekResponse(
  @SerializedName("week") val week: Int,
  @SerializedName("day") val day: Int,
)
