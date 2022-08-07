// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.model.karya

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.karyaplatform.karya.data.model.karya.enums.FileCreator

@Entity(tableName = "karya_file")
data class KaryaFileRecord(
  @PrimaryKey var id: String,
  var local_id: String,
  var box_id: Int?,
  var container_name: String,
  var name: String,
  var url: String?,
  var creator: FileCreator,
  var worker_id: String?,
  var algorithm: ChecksumAlgorithm,
  var checksum: String,
  var in_box: Boolean,
  var in_server: Boolean,
  var created_at: String,
  var last_updated_at: String,
)
