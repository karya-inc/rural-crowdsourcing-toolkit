package com.karyaplatform.karya.ui.onboarding.fileDownload

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.data.manager.AuthManager
import com.karyaplatform.karya.data.manager.ResourceManager
import com.karyaplatform.karya.ui.onboarding.accesscode.AccessCodeViewModel
import com.karyaplatform.karya.utils.Result
import com.karyaplatform.karya.utils.extensions.viewLifecycle
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.karyaplatform.karya.utils.extensions.observe
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FileDownloadFragment : Fragment(R.layout.fragment_file_download) {

  val viewModel by viewModels<AccessCodeViewModel>()

  @Inject lateinit var resourceManager: ResourceManager

  @Inject lateinit var authManager: AuthManager

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    downloadResourceFiles()
  }

  private fun downloadResourceFiles() {
    viewLifecycleScope.launch {
      val worker = authManager.getLoggedInWorker()

      val fileDownloadFlow = resourceManager.downloadLanguageResources(worker.accessCode, worker.language)

      fileDownloadFlow.observe(viewLifecycle, viewLifecycleScope) { result ->
        when (result) {
          is Result.Success<*> -> navigateToRegistration()
          is Result.Error -> {
            // Toast.makeText(requireContext(), "Could not download resources",
            // Toast.LENGTH_LONG).show()
            navigateToRegistration()
          }
          Result.Loading -> {}
        }
      }
    }
  }

  private fun navigateToRegistration() {
    findNavController().navigate(R.id.action_fileDownloadFragment_to_consentFormFragment)
  }
}
