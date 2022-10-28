// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
package com.karyaplatform.karya.data.model.karya.enums

import com.google.gson.annotations.SerializedName

enum class FileCreator {
  @SerializedName("WORKER") WORKER,
  @SerializedName("BOX") BOX,
  @SerializedName("SERVER") SERVER
}
