package com.karyaplatform.karya.data.remote.response

import com.google.gson.annotations.SerializedName
import com.karyaplatform.karya.data.model.karya.MicroTaskAssignmentRecord
import com.karyaplatform.karya.data.model.karya.MicroTaskRecord
import com.karyaplatform.karya.data.model.karya.TaskRecord

data class GetVerifiedAssignmentsResponse(
  @SerializedName("tasks") val tasks: List<TaskRecord>,
  @SerializedName("assignments") val assignments: List<MicroTaskAssignmentRecord>
)
