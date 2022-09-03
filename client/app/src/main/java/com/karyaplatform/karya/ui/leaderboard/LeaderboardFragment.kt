package com.karyaplatform.karya.ui.leaderboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentLeaderboardBinding
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.app_toolbar.*

@AndroidEntryPoint
class LeaderboardFragment : Fragment(R.layout.fragment_leaderboard) {
  private val binding by viewBinding(FragmentLeaderboardBinding::bind)
  private val viewModel by viewModels<LeaderboardViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupViews()
    observeUiState()
  }

  private fun setupViews() {
    toolbarBackBtn.visible()
    toolbarBackBtn.setOnClickListener {
      findNavController().popBackStack()
    }
    binding.leaderboardRv.adapter = LeaderboardListAdapter(emptyList())
  }

  private fun observeUiState() {
    with(binding) {
      viewModel.leaderboardList.observe(viewLifecycleOwner.lifecycle, lifecycleScope) { lbList ->
        if (lbList.isEmpty()) {
          leaderboardRv.gone()
          emptyLeaderboardTv.visible()
        } else {
          emptyLeaderboardTv.gone()
          leaderboardRv.visible()
          (leaderboardRv.adapter as LeaderboardListAdapter).updateList(lbList)
        }
      }
    }
  }
}