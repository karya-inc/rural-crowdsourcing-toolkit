// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.local.daosExtra

import androidx.room.Dao
import androidx.room.Query
import com.karyaplatform.karya.data.model.karya.enums.MicrotaskAssignmentStatus

@Dao
interface MicrotaskDaoExtra {
  @Query(
    "SELECT m.id FROM microtask AS m INNER JOIN microtask_assignment AS ma WHERE m.input_file_id IS NOT NULL AND ma.microtask_id = m.id AND ma.status in (:status)"
  )
  suspend fun getSubmittedMicrotasksWithInputFiles(
    status: List<MicrotaskAssignmentStatus> = arrayListOf(MicrotaskAssignmentStatus.SUBMITTED, MicrotaskAssignmentStatus.VERIFIED)
  ): List<String>
}
