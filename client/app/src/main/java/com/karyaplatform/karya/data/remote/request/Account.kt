package com.karyaplatform.karya.data.remote.request

import com.google.gson.annotations.SerializedName

data class Account(@SerializedName("id") val id: String?, @SerializedName("ifsc") val ifsc: String?)
