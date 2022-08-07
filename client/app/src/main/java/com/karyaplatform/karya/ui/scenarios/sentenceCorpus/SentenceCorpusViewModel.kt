package com.karyaplatform.karya.ui.scenarios.sentenceCorpus

import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.model.karya.enums.LanguageType
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import com.karyaplatform.karya.data.model.karya.enums.MicrotaskAssignmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
class SentenceCorpusViewModel
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  taskRepository: TaskRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir fileDirPath: String,
  authManager: AuthManager,
) : BaseMTRendererViewModel(
  assignmentRepository,
  taskRepository,
  microTaskRepository,
  fileDirPath,
  authManager
) {
  private val _contextText: MutableStateFlow<String> = MutableStateFlow("")
  val contextText = _contextText.asStateFlow()

  private val _sentences: MutableStateFlow<ArrayList<String>> = MutableStateFlow(ArrayList())
  val sentences = _sentences.asStateFlow()

  var limit by Delegates.notNull<Int>()

  override fun setupViewModel(taskId: String, completed: Int, total: Int) {
    super.setupViewModel(taskId, completed, total)
  }

  override fun setupMicrotask() {
    // TODO: Move to Gson
    val contextText = currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("prompt").asString
    _contextText.value = contextText

    limit = try {
      currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("limit").asInt
    } catch (e: Exception) {
      999999
    }

    // Reset sentence list
    _sentences.value = ArrayList()

    if (currentAssignment.status == MicrotaskAssignmentStatus.COMPLETED) {
      renderOutputData()
    }
  }

  private fun renderOutputData() {
    val outputData = currentAssignment.output.asJsonObject.getAsJsonObject("data")
    val sentences = outputData.getAsJsonObject("sentences")

    for (sentence in sentences.keySet()) {
      addSentence(sentence)
    }
  }

  /** Handle next button click */
  fun handleNextClick() {

    /** Log button press */
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "NEXT")
    log(message)

    val sentences = JsonObject()

    for (sentence in _sentences.value) {
      val status = JsonObject()
      status.addProperty("status", "UNKNOWN")
      sentences.add(sentence, status)
    }

    outputData.add("sentences", sentences)

    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }

  /** Handle next button click */
  fun handleBackClick() {
    moveToPreviousMicrotask()
  }

  fun addSentence(sentence: String) {
    val temp = ArrayList(_sentences.value)
    temp.add(0, sentence)
    _sentences.value = temp
  }

  fun removeSentenceAt(position: Int) {
    val temp = ArrayList(_sentences.value)
    temp.removeAt(position)
    _sentences.value = temp
  }

}
