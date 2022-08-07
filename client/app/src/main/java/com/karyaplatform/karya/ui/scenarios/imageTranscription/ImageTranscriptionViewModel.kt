package com.karyaplatform.karya.ui.scenarios.imageTranscription

import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.MicroTaskRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.injection.qualifier.FilesDir
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ImageTranscriptionViewModel
@Inject
constructor(
  assignmentRepository: AssignmentRepository,
  taskRepository: TaskRepository,
  microTaskRepository: MicroTaskRepository,
  @FilesDir fileDirPath: String,
  authManager: AuthManager,
) : BaseMTRendererViewModel(assignmentRepository, taskRepository, microTaskRepository, fileDirPath, authManager) {

  // Image to be shown
  private val _imageFilePath: MutableStateFlow<String> = MutableStateFlow("")
  val imageFilePath = _imageFilePath.asStateFlow()

  /** Complete microtask and move to next */
  fun completeTranscription(transcription: String) {
    // Add output transcription
    outputData.addProperty("transcription", transcription)
    viewModelScope.launch {
      completeAndSaveCurrentMicrotask()
      moveToNextMicrotask()
    }
  }

  /** Setup image transcription microtask */
  override fun setupMicrotask() {
    // Get and set the image file
    _imageFilePath.value =
      try {
        val imageFileName = currentMicroTask.input.asJsonObject.getAsJsonObject("files").get("image").asString
        microtaskInputContainer.getMicrotaskInputFilePath(currentMicroTask.id, imageFileName)
      } catch (e: Exception) {
        ""
      }
  }
}
