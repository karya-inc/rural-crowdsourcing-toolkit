package com.karyaplatform.karya.data.remote.response

import com.google.gson.annotations.SerializedName

data class Meta(
  @SerializedName("account") val account: Account,
  @SerializedName("name") val name: String,
  @SerializedName("failure_reason") val failure_reason: String?
)
