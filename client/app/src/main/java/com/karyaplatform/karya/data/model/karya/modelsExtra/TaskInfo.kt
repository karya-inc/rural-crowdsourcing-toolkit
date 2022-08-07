// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.data.model.karya.modelsExtra

import com.karyaplatform.karya.data.model.karya.enums.ScenarioType
import com.google.gson.JsonObject

data class TaskInfo(
  val taskID: String,
  val taskName: String,
  val taskInstruction: String?,
  val scenarioName: ScenarioType,
  val taskStatus: TaskStatus,
  val isGradeCard: Boolean,
  val reportSummary: JsonObject?,
)
