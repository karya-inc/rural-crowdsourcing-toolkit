// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.local.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.karyaplatform.karya.data.model.karya.TaskRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao : BasicDao<TaskRecord> {

  @Query("SELECT * FROM task") suspend fun getAll(): List<TaskRecord>

  @Query("SELECT * FROM task WHERE id == :id") suspend fun getById(id: String): TaskRecord

  @Query("SELECT * FROM task") fun getAllAsFlow(): Flow<List<TaskRecord>>

  /** Upsert a [record] in the table */
  @Transaction
  suspend fun upsert(record: TaskRecord) {
    insertForUpsert(record)
    updateForUpsert(record)
  }

  /** Upsert a list of [records] in the table */
  @Transaction
  suspend fun upsert(records: List<TaskRecord>) {
    insertForUpsert(records)
    updateForUpsert(records)
  }
}
