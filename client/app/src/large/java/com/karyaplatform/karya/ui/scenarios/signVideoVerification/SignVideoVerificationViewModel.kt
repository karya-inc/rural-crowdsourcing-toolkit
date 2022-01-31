package com.karyaplatform.karya.ui.scenarios.signVideoVerification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonObject
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import com.karyaplatform.karya.ui.scenarios.signVideoVerification.SignVideoVerificationViewModel.ButtonState.DISABLED
import com.karyaplatform.karya.ui.scenarios.signVideoVerification.SignVideoVerificationViewModel.ButtonState.ENABLED
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SignVideoVerificationViewModel
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
  authManager) {

  var score: Int = 0
  var remarks: String = ""

  /** UI State **/
  private val _backBtnState: MutableStateFlow<ButtonState> = MutableStateFlow(ENABLED)
  val backBtnState = _backBtnState.asStateFlow()

  private val _nextBtnState: MutableStateFlow<ButtonState> = MutableStateFlow(ENABLED)
  val nextBtnState = _nextBtnState.asStateFlow()

  private val _oldRemarks: MutableStateFlow<String> = MutableStateFlow("")
  val oldRemarks = _oldRemarks.asStateFlow()

  private val _oldScore: MutableStateFlow<Int> = MutableStateFlow(0)
  val oldScore = _oldScore.asStateFlow()

  private val _sentenceTvText: MutableStateFlow<String> = MutableStateFlow("")
  val sentenceTvText = _sentenceTvText.asStateFlow()

  private val _recordingFile: MutableStateFlow<String> = MutableStateFlow("")
  val recordingFile = _recordingFile.asStateFlow()

  private val _videoPlayerVisibility: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val videoPlayerVisibility = _videoPlayerVisibility.asStateFlow()


  /**
   * UI button states
   *
   * [DISABLED]: Greyed out. Cannot click [ENABLED]: Can click
   */
  enum class ButtonState {
    DISABLED,
    ENABLED
  }



  /** Shortcut to set and flush all four button states (in sequence) */
  private fun setButtonStates(b: ButtonState, n: ButtonState) {
    _backBtnState.value = b
    _nextBtnState.value = n
  }

  /** Handle next button click */
  fun handleNextClick() {
    // log the state transition
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "NEXT")
    log(message)

    outputData.addProperty("score", score)
    outputData.addProperty("remarks", remarks)

    _videoPlayerVisibility.value = false

    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
    resetStates()
  }

  private fun resetStates() {
    score = 0
    remarks = ""
  }

  /** Handle back button click */
  fun handleBackClick() {
    // log the state transition
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "BACK")
    log(message)

    moveToPreviousMicrotask()
  }

  fun onBackPressed() {
    // log the state transition
    val message = JsonObject()
    message.addProperty("type", "o")
    message.addProperty("button", "ANDROID_BACK")
    log(message)
    navigateBack()
  }


  override fun setupMicrotask() {

    // TODO: Pick up from server
    val sentence = currentMicroTask.input.asJsonObject.getAsJsonObject("data").get("sentence").toString()

    try {
      val recordingFileName =
        currentMicroTask.input.asJsonObject.getAsJsonObject("files").get("recording").asString
      val recordFilePath = microtaskInputContainer.getMicrotaskInputFilePath(
        currentMicroTask.id,
        recordingFileName
      )

      _recordingFile.value = recordFilePath
      _videoPlayerVisibility.value = true
      _sentenceTvText.value = sentence
    } catch (e: Exception) {
      FirebaseCrashlytics.getInstance().recordException(e)
      // log the state transition
      val message = JsonObject()
      message.addProperty("type", "m")
      message.addProperty("message", "NO_FILE")
      log(message)

      outputData.addProperty("score", 0)
      outputData.addProperty("remarks", "No recording file present")

      _videoPlayerVisibility.value = false

      viewModelScope.launch {
        completeAndSaveCurrentMicrotask()
        moveToNextMicrotask()
      }
      resetStates()
    }
  }

}
