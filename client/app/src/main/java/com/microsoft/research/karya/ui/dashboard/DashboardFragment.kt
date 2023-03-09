package com.microsoft.research.karya.ui.dashboard

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.microsoft.research.karya.BuildConfig
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.model.karya.enums.ScenarioType
import com.microsoft.research.karya.data.model.karya.modelsExtra.TaskInfo
import com.microsoft.research.karya.databinding.FragmentDashboardBinding
import com.microsoft.research.karya.ui.base.SessionFragment
import com.microsoft.research.karya.utils.Constants.ALLOW_AFTER_TIMEOUT_DURATION_MILLIS
import com.microsoft.research.karya.utils.PreferenceKeys
import com.microsoft.research.karya.utils.extensions.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.app_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val UNIQUE_SYNC_WORK_NAME = "syncWork"

enum class ERROR_TYPE {
  SYNC_ERROR, TASK_ERROR
}

enum class ERROR_LVL {
  WARNING, ERROR
}

@AndroidEntryPoint
class DashboardFragment : SessionFragment(R.layout.fragment_dashboard) {

  val binding by viewBinding(FragmentDashboardBinding::bind)
  val viewModel: DashboardViewModel by viewModels()
  private lateinit var syncWorkRequest: OneTimeWorkRequest

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    setupWorkRequests()
    observeUi()
    // Check if dashboard is visited the first time
    viewLifecycleScope.launch {
      val firstRunKey = booleanPreferencesKey(PreferenceKeys.IS_FIRST_DASHBOARD_VISIT)
      val datastore = requireContext().dataStore
      val data = datastore.data.first()
      val isFirstTime = data[firstRunKey] ?: true
      if (isFirstTime) onFirstTimeVisit()
      datastore.edit { prefs -> prefs[firstRunKey] = false }
      updateWorkWeek()
    }
  }

  // Function is invoked when the fragment is run for the first time
  private fun onFirstTimeVisit() {
    syncWithServer()
  }

  private suspend fun updateWorkWeek() {
    val weekDayTriple = getWorkerWeekAndDay()
    val week = weekDayTriple.first
    val day = weekDayTriple.second
    val regTime = weekDayTriple.third
    // Set Views
    binding.weekTv.text = if (regTime > 0) week.toString() else "-"
    binding.dayTv.text = if (regTime > 0) day.toString() else "-"
  }

  private suspend fun getWorkerWeekAndDay(): Triple<Long, Long, Long> {
    val datastore = requireContext().dataStore
    val data = datastore.data.first()

    // Get Worker's Week and Day from datastore
    val regTimeKey = stringPreferencesKey(PreferenceKeys.REG_TIME)
    val weekKey = intPreferencesKey(PreferenceKeys.CURRENT_WEEK)
    val dayKey = intPreferencesKey(PreferenceKeys.CURRENT_DAY)

    val regTime = (data[regTimeKey] ?: "0").toLong()
    val currentTime = System.currentTimeMillis()
    val diff = currentTime - regTime
    val ngWeek = if (regTime == 0L) 0 else (diff / 1000 / 60 / 60 / 24 / 7) + 1
    val ngDay = if (regTime == 0L) 0 else ((diff / 1000 / 60 / 60 / 24) % 7) + 1

    return Triple(ngWeek, ngDay, regTime)
  }

  private fun observeUi() {
    viewModel.dashboardUiState.observe(lifecycle, lifecycleScope) { dashboardUiState ->
      when (dashboardUiState) {
        is DashboardUiState.Success -> showSuccessUi(dashboardUiState.data)
        is DashboardUiState.Error -> showErrorUi(
          dashboardUiState.throwable,
          ERROR_TYPE.TASK_ERROR,
          ERROR_LVL.ERROR
        )
        DashboardUiState.Loading -> showLoadingUi()
      }
    }

    viewModel.workerAccessCode.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { code ->
      binding.accessCodeTv.text = code
    }

    // Work for center user
    viewModel.workFromCenterUser.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { wfc ->
      if (wfc) {
        if (!viewModel.userInCenter.value) {
          binding.wfcEnterCodeLL.visible()
          binding.revokeWFCAuthorizationBtn.gone()
        } else {
          binding.wfcEnterCodeLL.gone()
          binding.revokeWFCAuthorizationBtn.visible()
        }
      }
    }

    viewModel.userInCenter.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { uIC ->
      if (viewModel.workFromCenterUser.value) {
        if (!uIC) {
          binding.wfcEnterCodeLL.visible()
          binding.revokeWFCAuthorizationBtn.gone()
        } else {
          binding.wfcEnterCodeLL.gone()
          binding.revokeWFCAuthorizationBtn.visible()
        }
      }
    }

    viewModel.progress.observe(lifecycle, lifecycleScope) { i ->
      binding.syncProgressBar.progress = i
      viewModel.progress.observe(lifecycle, lifecycleScope) { i -> binding.syncProgressBar.progress = i }

      WorkManager.getInstance(requireContext())
        .getWorkInfosForUniqueWorkLiveData(UNIQUE_SYNC_WORK_NAME)
        .observe(viewLifecycleOwner) { workInfos ->
          if (workInfos.size == 0) return@observe // Return if the workInfo List is empty
          val workInfo = workInfos[0] // Picking the first workInfo
          if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
            lifecycleScope.launch {
              val warningMsg = workInfo.outputData.getString("warningMsg")
              if (warningMsg != null) { // Check if there are any warning messages set by Workmanager
                showErrorUi(Throwable(warningMsg), ERROR_TYPE.SYNC_ERROR, ERROR_LVL.WARNING)
              }
              viewModel.setProgress(100)
              viewModel.refreshList()
            }
          }
          if (workInfo != null && workInfo.state == WorkInfo.State.ENQUEUED) {
            viewModel.setProgress(0)
            viewModel.setLoading()
          }
          if (workInfo != null && workInfo.state == WorkInfo.State.RUNNING) {
            // Check if the current work's state is "successfully finished"
            val progress: Int = workInfo.progress.getInt("progress", 0)
            viewModel.setProgress(progress)
            viewModel.setLoading()
            // refresh the UI to show microtasks
            if (progress == 100)
              viewLifecycleScope.launch {
                viewModel.refreshList()
                updateWorkWeek()
              }
          }
          if (workInfo != null && workInfo.state == WorkInfo.State.FAILED) {
            lifecycleScope.launch {
              showErrorUi(
                Throwable(workInfo.outputData.getString("errorMsg")),
                ERROR_TYPE.SYNC_ERROR,
                ERROR_LVL.ERROR
              )
              viewModel.refreshList()
              updateWorkWeek()
            }
          }
        }
    }
  }

  private suspend fun checkToShowTaskCompletionMessage(taskInfoData: List<TaskInfo>) {
    var totalMicrotaskCount = 0
    taskInfoData.forEach { taskInfo ->
      val taskMicrotaskCount =  with(taskInfo.taskStatus) {completedMicrotasks + expiredMicrotasks + skippedMicrotasks + submittedMicrotasks}
      totalMicrotaskCount += taskMicrotaskCount
    }
    val weekTriple = getWorkerWeekAndDay()
    val week = weekTriple.first
    when(week) {
      1L -> if (totalMicrotaskCount >= 2604) showDialogueForTaskCompletion(R.string.week_task_completed_msg)
      2L -> if (totalMicrotaskCount >= 5883) showDialogueForTaskCompletion(R.string.week_task_completed_msg)
      3L -> if (totalMicrotaskCount >= 9162) showDialogueForTaskCompletion(R.string.week_task_completed_msg)
      4L -> if (totalMicrotaskCount >= 12441) showDialogueForTaskCompletion(R.string.week_task_completed_msg)
      5L -> if (totalMicrotaskCount >= 15520) showDialogueForTaskCompletion(R.string.week_task_completed_msg)
      else -> if (totalMicrotaskCount >= 15520) showDialogueForTaskCompletion(R.string.study_completed_msg)
    }
  }

  override fun onSessionExpired() {
    WorkManager.getInstance(requireContext()).cancelAllWork()
    super.onSessionExpired()
  }

  override fun onResume() {
    super.onResume()
    viewModel.getAllTasks() // TODO: Remove onResume and get taskId from scenario viewmodel (similar to onActivity Result)
  }

  private fun setupWorkRequests() {
    // TODO: SHIFT IT FROM HERE
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    syncWorkRequest = OneTimeWorkRequestBuilder<DashboardSyncWorker>()
      .setConstraints(constraints)
      .build()
  }

  private fun setupViews() {

    toolbarBackBtn.visible()

    toolbarBackBtn.setOnClickListener {
      findNavController().popBackStack()
    }

    with(binding) {
      tasksRv.apply {
        adapter = TaskListAdapter(emptyList(), ::onDashboardItemClick)
        layoutManager = LinearLayoutManager(context)
      }

      binding.syncCv.setOnClickListener { syncWithServer() }

      binding.submitCenterCodeBtn.setOnClickListener {
        viewModel.authorizeWorkFromCenterUser(binding.centerCode.text.toString())
        binding.centerCode.text.clear()
      }

      binding.revokeWFCAuthorizationBtn.setOnClickListener {
        viewLifecycleScope.launch {
          viewModel.revokeWFCAuthorization()
          binding.centerCode.text.clear()
        }
      }

      loadProfilePic()
    }
  }

  private fun syncWithServer() {
    setupWorkRequests()
    WorkManager.getInstance(requireContext())
      .enqueueUniqueWork(UNIQUE_SYNC_WORK_NAME, ExistingWorkPolicy.KEEP, syncWorkRequest)
  }

  private fun showSuccessUi(data: DashboardStateSuccess) {
    WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(syncWorkRequest.id)
      .observe(viewLifecycleOwner, Observer { workInfo ->
        if (workInfo == null || workInfo.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.FAILED) {
          hideLoading() // Only hide loading if no work is in queue
        }
      })
    binding.syncCv.enable()
    data.apply {
      (binding.tasksRv.adapter as TaskListAdapter).updateList(taskInfoData)
    }

    // Show a dialog box to sync with server if completed tasks and internet available
    if (requireContext().isNetworkAvailable()) {
      for (taskInfo in data.taskInfoData) {
        if (taskInfo.taskStatus.completedMicrotasks > 0) {
          showDialogueToSync()
          return
        }
      }
    }
  }

  private fun showDialogueToSync() {
    var dialog: AlertDialog? = null
    if (dialog != null && dialog!!.isShowing) return

    val builder: AlertDialog.Builder? = activity?.let {
      AlertDialog.Builder(it)
    }

    builder?.setMessage(R.string.sync_prompt_message)

    // Set buttons
    builder?.apply {
      setPositiveButton(
        R.string.yes
      ) { _, _ ->
        syncWithServer()
        dialog!!.dismiss()
      }
      setNegativeButton(R.string.no, null)
    }

    dialog = builder?.create()
    dialog!!.show()
  }

  private fun showDialogueForTaskCompletion(msgId: Int) {
    var dialog: AlertDialog? = null
    if (dialog != null && dialog!!.isShowing) return

    val builder: AlertDialog.Builder? = activity?.let {
      AlertDialog.Builder(it)
    }

    // Set buttons
    builder?.apply {
      setPositiveButton(
        R.string.okay, null
      )
    }

    builder?.setMessage(msgId)

    dialog = builder?.create()
    dialog!!.show()
  }

  private fun showErrorUi(throwable: Throwable, errorType: ERROR_TYPE, errorLvl: ERROR_LVL) {
    hideLoading()
    showError(throwable.message ?: "Some error Occurred", errorType, errorLvl)
    binding.syncCv.enable()
  }

  private fun showError(message: String, errorType: ERROR_TYPE, errorLvl: ERROR_LVL) {
    if (errorType == ERROR_TYPE.SYNC_ERROR) {
      WorkManager.getInstance(requireContext()).cancelAllWork()
      with(binding) {
        syncErrorMessageTv.text = message

        when (errorLvl) {
          ERROR_LVL.ERROR -> syncErrorMessageTv.setTextColor(Color.RED)
          ERROR_LVL.WARNING -> syncErrorMessageTv.setTextColor(Color.YELLOW)
        }
        syncErrorMessageTv.visible()
      }
    }
  }

  private fun showLoadingUi() {
    showLoading()
    binding.syncCv.disable()
    binding.syncErrorMessageTv.gone()
  }

  private fun showLoading() = binding.syncProgressBar.visible()

  private fun hideLoading() = binding.syncProgressBar.gone()

  private fun loadProfilePic() {
    binding.appTb.showProfilePicture()

    lifecycleScope.launchWhenStarted {
      withContext(Dispatchers.IO) {
        val profilePicPath =
          authManager.getLoggedInWorker().profilePicturePath ?: return@withContext
        val bitmap = BitmapFactory.decodeFile(profilePicPath)

        withContext(Dispatchers.Main.immediate) { binding.appTb.setProfilePicture(bitmap) }
      }
    }
  }

  private fun onDashboardItemClick(task: TaskInfo) {

    val timeoutHappenedAt = runBlocking { requireContext().dataStore.data.first()[PreferenceKeys.INACTIVITY_TIMEOUT] }
    // Timeout is not null and duration within 10 minutes, then don't navigate to task
    val currentTime = System.currentTimeMillis()
    if (timeoutHappenedAt != null && currentTime - timeoutHappenedAt < ALLOW_AFTER_TIMEOUT_DURATION_MILLIS) {
      val diffMillis = (currentTime - timeoutHappenedAt)
      val remainingTime = ALLOW_AFTER_TIMEOUT_DURATION_MILLIS - diffMillis // 10 minutes - difference
      Toast.makeText(
        requireContext(),
        "You've been noticed to crossed maximum timeout limit, please try again after ${remainingTime / 1000} seconds!",
        Toast.LENGTH_SHORT
      ).show()
      return
    }

    if (!task.isGradeCard && (task.taskStatus.assignedMicrotasks + task.taskStatus.skippedMicrotasks) > 0) {
      val taskId = task.taskID
      val status = task.taskStatus
      val completed =
        status.completedMicrotasks + status.submittedMicrotasks + status.verifiedMicrotasks
      val total = status.assignedMicrotasks + completed
      var action = with(DashboardFragmentDirections) {
        when (task.scenarioName) {
          ScenarioType.SPEECH_DATA -> actionDashboardActivityToSpeechDataMainFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.XLITERATION_DATA -> actionDashboardActivityToUniversalTransliterationMainFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.SPEECH_VERIFICATION -> actionDashboardActivityToSpeechVerificationFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.IMAGE_TRANSCRIPTION -> actionDashboardActivityToImageTranscription(
            taskId,
            completed,
            total
          )
          ScenarioType.IMAGE_LABELLING -> actionDashboardActivityToImageLabelling(
            taskId,
            completed,
            total
          )
          ScenarioType.IMAGE_ANNOTATION -> actionDashboardActivityToImageAnnotationFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.QUIZ -> actionDashboardActivityToQuiz(taskId, completed, total)
          ScenarioType.IMAGE_DATA -> actionDashboardActivityToImageData(taskId, completed, total)
          ScenarioType.SENTENCE_VALIDATION -> actionDashboardActivityToSentenceValidation(
            taskId,
            completed,
            total
          )
          ScenarioType.SPEECH_TRANSCRIPTION -> actionDashboardActivityToSpeechTranscriptionFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.SENTENCE_CORPUS -> actionDashboardActivityToSentenceCorpusFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.IMAGE_ANNOTATION_VALIDATION -> actionDashboardActivityToImageAnnotationVerificationFragment(
            taskId,
            completed,
            total
          )
          ScenarioType.SENTENCE_CORPUS_VERIFICATION -> actionDashboardActivityToSentenceCorpusVerificationFragment(
            taskId,
            completed,
            total
          )
          else -> null
        }
      }
      if (action == null && BuildConfig.FLAVOR == "large") {
        action = when (task.scenarioName) {
          ScenarioType.SIGN_LANGUAGE_VIDEO -> DashboardFragmentDirections.actionDashboardActivityToSignVideo(
            taskId,
            completed,
            total
          )
          ScenarioType.SGN_LANG_VIDEO_VERIFICATION -> DashboardFragmentDirections.actionDashboardActivityToSignVideoVerification(
            taskId,
            completed,
            total
          )
          else -> null
        }
      }
      if (action != null) {
        viewLifecycleScope.launch {
          // Check if user is in center
          if (viewModel.workFromCenterUser.value) {
            if (!arrayOf("70", "71", "72", "73").contains(taskId)  && !viewModel.checkWorkFromCenterUserAuth()) {
              binding.centerCode.requestFocus()
              return@launch
            }
          }

          findNavController().navigate(action)
        }
      }
    }
  }
}
