package com.karyaplatform.karya.ui.scenarios.signVideo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.karyaplatform.karya.R
import com.karyaplatform.karya.ui.scenarios.common.BaseMTRendererFragment
import com.karyaplatform.karya.ui.scenarios.signVideo.SignVideoMainViewModel.ButtonState.DISABLED
import com.karyaplatform.karya.ui.scenarios.signVideo.SignVideoMainViewModel.ButtonState.ENABLED
import com.karyaplatform.karya.utils.extensions.invisible
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import com.potyvideo.library.globalInterfaces.AndExoPlayerListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.microtask_common_back_button.view.*
import kotlinx.android.synthetic.main.microtask_common_next_button.view.*
import kotlinx.android.synthetic.main.microtask_sign_video_data.*

@AndroidEntryPoint
class SignVideoMainFragment : BaseMTRendererFragment(R.layout.microtask_sign_video_data) {
  override val viewModel: SignVideoMainViewModel by viewModels()
  val args: SignVideoMainFragmentArgs by navArgs()

  private val recordVideoLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

      if (result.resultCode == AppCompatActivity.RESULT_OK) {
        viewModel.setVideoSource(viewModel.outputRecordingFilePath)

        viewModel.onVideoReceived()

        videoPlayer.startPlayer()
        videoPlayer.setShowControllers(false)
        videoPlayer.setAndExoPlayerListener(object : AndExoPlayerListener {
          override fun onExoEnded() {
            super.onExoEnded()
            viewModel.onPlayerEnded()
          }
        })
      }
    }

  override fun requiredPermissions(): Array<String> {
    return arrayOf(Manifest.permission.CAMERA)
  }

  private fun setupObservers() {

    viewModel.videoSource.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { source ->
      if (source.isNotEmpty()) {
        videoPlayer.setSource(source)
      }
    }

    viewModel.backBtnState.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { state ->
      backBtnCv.isClickable = state != DISABLED
      backBtnCv.backIv.setBackgroundResource(
        when (state) {
          DISABLED -> R.drawable.ic_back_disabled
          ENABLED -> R.drawable.ic_back_enabled
        }
      )
    }

    viewModel.recordBtnState.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { state ->
      recordBtn.isClickable = state != DISABLED
      recordBtn.alpha =
        when (state) {
          DISABLED -> 0.5F
          ENABLED -> 1F
        }
    }

    viewModel.nextBtnState.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { state ->
      nextBtnCv.isClickable = state != DISABLED
      nextBtnCv.nextIv.setBackgroundResource(
        when (state) {
          DISABLED -> R.drawable.ic_next_disabled
          ENABLED -> R.drawable.ic_next_enabled
        }
      )
    }

    viewModel.launchRecordVideo.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) {
      /** Determine action based on current state */
      val intent = Intent(requireContext(), SignVideoRecord::class.java)
      intent.putExtra("video_file_path", viewModel.outputRecordingFilePath)
      recordVideoLauncher.launch(intent)
    }

    viewModel.videoPlayerVisibility.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { visible ->
      if (visible) {
        showVideoPlayer()
      } else {
        hideVideoPlayer()
      }
    }

    viewModel.sentenceTvText.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { text ->
      sentenceTv.text = text
    }


  }

  private fun showVideoPlayer() {
    videoPlayer.visible()
    videoPlayerPlaceHolder.invisible()
  }

  private fun hideVideoPlayer() {
    videoPlayer.invisible()
    videoPlayerPlaceHolder.visible()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    viewModel.setupViewModel(args.taskId, args.completed, args.total)
    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupObservers()
    /** Set OnBackPressed callback */
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { viewModel.onBackPressed() }

    /** record instruction */
    val recordInstruction =
      viewModel.task.params.asJsonObject.get("instruction").asString
        ?: "TEST INSTRUCTION (HARDCODED)"
    instructionTv.text = recordInstruction

    /** Set on click listeners */
    recordBtn.setOnClickListener {
      viewModel.handleRecordClick()
    }
    nextBtnCv.setOnClickListener { viewModel.handleNextClick() }
    backBtnCv.setOnClickListener { viewModel.handleBackClick() }
  }
}

