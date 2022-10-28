package com.karyaplatform.karya.ui.scenarios.sentenceCorpusVerification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SentenceCorpusVerificationViewModel
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  taskRepository: TaskRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir fileDirPath: String,
  authManager: AuthManager,
  dataStore: DataStore<Preferences>
) : BaseMTRendererViewModel(
  assignmentRepository,
  taskRepository,
  microTaskRepository,
  fileDirPath,
  authManager,
  dataStore
) {
  private val _contextText: MutableStateFlow<String> = MutableStateFlow("")
  val contextText = _contextText.asStateFlow()

  private val _sentences: MutableStateFlow<HashMap<String, String?>> = MutableStateFlow(HashMap())
  val sentences = _sentences.asStateFlow()

  override fun setupMicrotask() {
    // TODO: Move to Gson
    val contextText = currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("prompt").asString
    _contextText.value = contextText

    // Get sentence list
    val sentencesJsonObject = currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("sentences").asJsonObject

    val sentences = HashMap<String, String?>()
    for(ele in sentencesJsonObject.entrySet()) {
      sentences[ele.key] = ele.value.asJsonObject.get("status").asString
    }
    _sentences.value = sentences
  }

  /** Handle next button click */
  fun handleNextClick() {

    /** Log button press */
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "NEXT")
    log(message)

    val sentences = JsonObject()

    for (entry in _sentences.value) {
      val sentence = entry.key
      val status = entry.value
      val statusObject = JsonObject()
      statusObject.addProperty("status", status)
      sentences.add(sentence, statusObject)
    }

    outputData.add("sentences", sentences)

    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }

}
