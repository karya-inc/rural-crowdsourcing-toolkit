// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.local.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.karyaplatform.karya.data.model.karya.KaryaFileRecord

@Dao
interface KaryaFileDao : BasicDao<KaryaFileRecord> {

  @Query("SELECT * FROM karya_file") suspend fun getAll(): List<KaryaFileRecord>

  @Query("SELECT * FROM karya_file WHERE id == :id") suspend fun getById(id: String): KaryaFileRecord

  /** Upsert a [record] in the table */
  @Transaction
  suspend fun upsert(record: KaryaFileRecord) {
    insertForUpsert(record)
    updateForUpsert(record)
  }

  /** Upsert a list of [records] in the table */
  @Transaction
  suspend fun upsert(records: List<KaryaFileRecord>) {
    insertForUpsert(records)
    updateForUpsert(records)
  }
}
