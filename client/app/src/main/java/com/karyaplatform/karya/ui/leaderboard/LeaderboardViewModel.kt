package com.karyaplatform.karya.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karyaplatform.karya.data.model.karya.LeaderboardRecord
import com.karyaplatform.karya.data.repo.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel
@Inject constructor(
  workerRepository: WorkerRepository
)
  : ViewModel() {

  // Leaderboard list
  private var _leaderboardList: MutableStateFlow<List<LeaderboardRecord>> =
    MutableStateFlow(arrayListOf())
  val leaderboardList = _leaderboardList.asStateFlow()

  // On init, fetch and set leaderboard entries
  init {
    viewModelScope.launch {
      _leaderboardList.value = workerRepository.getAllLeaderBoardRecords()
    }
  }
}