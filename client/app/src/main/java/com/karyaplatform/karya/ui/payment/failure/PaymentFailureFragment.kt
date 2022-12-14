package com.karyaplatform.karya.ui.payment.failure

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentPaymentFailureBinding
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.viewLifecycle
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentFailureFragment : Fragment(R.layout.fragment_payment_failure) {
  private val binding by viewBinding(FragmentPaymentFailureBinding::bind)
  private val viewModel by viewModels<PaymentFailureViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupListeners()
  }

  private fun setupListeners() {
    binding.registerButton.setOnClickListener { navigateToRegistration() }

    binding.backButton.setOnClickListener { navigateToDashboard() }

    viewModel.uiStateFlow.observe(viewLifecycle, viewLifecycleScope) { paymentModel -> render(paymentModel) }

    viewModel
      .navigationFlow
      .onEach { navigation ->
        when (navigation) {
          PaymentFailureNavigation.DASHBOARD -> navigateToDashboard()
          PaymentFailureNavigation.REGISTRATION -> navigateToRegistration()
        }
      }
      .launchIn(viewLifecycleScope)
  }

  private fun render(paymentVerificationModel: PaymentFailureModel) {
    viewLifecycleScope.launch {
      val failureReason = viewModel.getFailureReason()
      val baseMsg = getString(R.string.payment_registration_failure_message)
      if (!failureReason.isNullOrEmpty()) {
        binding.description.text = "$baseMsg\nReason: $failureReason"
      } else {
        binding.description.text = baseMsg
      }
    }
  }

  private fun navigateToDashboard() {
    findNavController().navigate(R.id.action_paymentFailureFragment_to_homeScreen)
  }

  private fun navigateToRegistration() {
    findNavController().navigate(R.id.action_paymentFailureFragment_to_paymentRegistrationFragment)
  }

  companion object {
    fun newInstance() = PaymentFailureFragment()
  }
}
