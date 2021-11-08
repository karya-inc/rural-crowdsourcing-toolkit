// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.remote.request

data class UploadFileRequest(
  var container_name: String,
  var name: String,
  var algorithm: String,
  var checksum: String,
)
