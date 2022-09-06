package com.karyaplatform.karya.ui.homeScreen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.model.karya.WorkerRecord
import com.karyaplatform.karya.data.model.karya.enums.ScenarioType
import com.karyaplatform.karya.data.model.karya.modelsExtra.EarningStatus
import com.karyaplatform.karya.data.model.karya.modelsExtra.PerformanceSummary
import com.karyaplatform.karya.data.model.karya.modelsExtra.TaskStatus
import com.karyaplatform.karya.data.repo.AssignmentRepository
import com.karyaplatform.karya.data.repo.PaymentRepository
import com.karyaplatform.karya.data.repo.TaskRepository
import com.karyaplatform.karya.data.repo.WorkerRepository
import com.karyaplatform.karya.ui.payment.PaymentFlowNavigation
import com.karyaplatform.karya.utils.PreferenceKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel
@Inject
constructor(
  private val taskRepository: TaskRepository,
  private val assignmentRepository: AssignmentRepository,
  private val authManager: AuthManager,
  private val workerRepository: WorkerRepository,
  private val paymentRepository: PaymentRepository,
) : ViewModel() {

  private lateinit var worker: WorkerRecord

  // Worker details
  private var _name = MutableStateFlow("")
  val name = _name.asStateFlow()

  private var _phoneNumber = MutableStateFlow("")
  val phoneNumber = _phoneNumber.asStateFlow()

  // XP points
  private var _points = MutableStateFlow(0)
  val points = _points.asStateFlow()

  // Navigation flow for payments
  private val _navigationFlow = MutableSharedFlow<HomeScreenNavigation>()
  val navigationFlow = _navigationFlow.asSharedFlow()

  // Task summary
  private var _taskSummary = MutableStateFlow(
    TaskStatus(
      0,
      0,
      0,
      0,
      0,
      0
    )
  )
  val taskSummary = _taskSummary.asStateFlow()

  // Performance summary
  private var _performanceSummary = MutableStateFlow(
    PerformanceSummary(0.0f, 0.0f, 0.0f, 0.0f)
  )
  val performanceSummary = _performanceSummary.asStateFlow()

  // Earnings summary
  private var _earningStatus = MutableStateFlow(EarningStatus(0.0f, 0.0f, 0.0f))
  val earningStatus = _earningStatus.asStateFlow()

  init {
    refreshWorker()
    refreshXPPoints()
    refreshTaskSummary()
    refreshPerformanceSummary()
    setEarningSummary()
  }

  private fun refreshWorker() {
    viewModelScope.launch {
      worker = authManager.getLoggedInWorker()
      val name = try {
        worker.profile!!.asJsonObject.get("name").asString
      } catch (e: Exception) {
        "No Name"
      }

      val phoneNumber = try {
        worker.phoneNumber!!
      } catch (e: Exception) {
        "9999999999"
      }

      _name.value = name
      _phoneNumber.value = phoneNumber
    }
  }

  fun refreshXPPoints() {
    if (this::worker.isInitialized) {
      viewModelScope.launch {
        _points.value = try {
          workerRepository.getXPPoints(worker.id)!!
        } catch (e: Exception) {
          0
        }
      }
    }
  }

  fun refreshTaskSummary() {
    viewModelScope.launch {
      _taskSummary.value = taskRepository.getTaskSummary()
    }
  }

  fun refreshPerformanceSummary() {
    viewModelScope.launch {
      val w = authManager.getLoggedInWorker()
      val scenarioSummary = assignmentRepository.getScenarioReportSummary(w.id)

      val recordingAccuracy = try {
        scenarioSummary[ScenarioType.SPEECH_DATA]!!.get("accuracy").asFloat
      } catch (e: Exception) {
        0.0f
      }
      val transcriptionAccuracy = try {
        scenarioSummary[ScenarioType.SPEECH_TRANSCRIPTION]!!.get("accuracy").asFloat
      } catch (e: Exception) {
        0.0f
      }
      val typingAccuracy = try {
        scenarioSummary[ScenarioType.SENTENCE_CORPUS]!!.get("accuracy").asFloat
      } catch (e: Exception) {
        0.0f
      }
      val imageAnnotationAccuracy = try {
        scenarioSummary[ScenarioType.IMAGE_ANNOTATION]!!.get("accuracy").asFloat
      } catch (e: Exception) {
        0.0f
      }

      _performanceSummary.value = PerformanceSummary(
        recordingAccuracy,
        transcriptionAccuracy,
        typingAccuracy,
        imageAnnotationAccuracy
      )
    }
  }

  fun setEarningSummary() {
    viewModelScope.launch {
      _earningStatus.value = paymentRepository.getWorkerEarnings()
    }
  }

  fun navigatePayment() {
    viewModelScope.launch {
      val workerId = authManager.getLoggedInWorker().id
      val status = paymentRepository.getPaymentRecordStatus(workerId)
      when (status.getNavigationDestination()) {
        PaymentFlowNavigation.DASHBOARD -> _navigationFlow.emit(HomeScreenNavigation.PAYMENT_DASHBOARD)
        PaymentFlowNavigation.FAILURE -> _navigationFlow.emit(HomeScreenNavigation.PAYMENT_FAILURE)
        PaymentFlowNavigation.REGISTRATION -> _navigationFlow.emit(HomeScreenNavigation.PAYMENT_REGISTRATION)
        PaymentFlowNavigation.VERIFICATION -> _navigationFlow.emit(HomeScreenNavigation.PAYMENT_VERIFICATION)
        else -> {}
      }
    }
  }
}
