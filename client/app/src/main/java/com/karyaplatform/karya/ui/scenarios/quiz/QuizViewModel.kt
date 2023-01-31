package com.karyaplatform.karya.ui.scenarios.quiz

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
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
class QuizViewModel
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

  // UI Elements controlled by the view model

  // Question
  private val _question: MutableStateFlow<Question> =
    MutableStateFlow(Question(QuestionType.invalid))
  val question = _question.asStateFlow()

  // Text response
  private val _textResponse: MutableStateFlow<String> = MutableStateFlow("")

  // MCQ response
  private val _mcqResponse: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

  /**
   * Setup quiz microtask
   */
  override fun setupMicrotask() {
    // Parse question from microtask input
    val inputData = currentMicroTask.input.asJsonObject.getAsJsonObject("data")
    _question.value = Gson().fromJson(inputData, Question::class.java)
  }

  /**
   * Update text response
   */
  fun updateTextResponse(res: String) {
    _textResponse.value = res
  }

  /**
   * Update mcq response
   */
  fun updateMCQResponse(value: String, checked: Boolean) {
    val current: MutableList<String> = mutableListOf()
    current.addAll(_mcqResponse.value.filter { v -> v != value })
    if (checked) current.add(value)
    _mcqResponse.value = current.toList()
  }

  /**
   * Submit the response and move to next task
   */
  fun submitResponse() {
    val key = _question.value.key
    when (_question.value.type) {
      QuestionType.text -> {
        outputData.addProperty(key, _textResponse.value)
      }
      QuestionType.mcq -> {
        val result = JsonArray()
        _mcqResponse.value.forEach { v -> result.add(v) }
        outputData.add(key, result)
      }
      else -> "invalid"
    }

    // Clear out response
    _textResponse.value = ""
    _mcqResponse.value = listOf()

    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }
}
