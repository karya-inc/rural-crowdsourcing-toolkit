package com.karyaplatform.karya.ui.payment.details.upi

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentPaymentDetailsUpiBinding
import com.karyaplatform.karya.ui.payment.details.PaymentDetailNavigation
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.hideKeyboard
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class PaymentDetailUPIFragment : Fragment(R.layout.fragment_payment_details_upi) {

  private val binding by viewBinding(FragmentPaymentDetailsUpiBinding::bind)
  private val viewModel by viewModels<PaymentDetailUPIViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupListeners()
  }

  private fun setupListeners() {
    binding.submitButton.setOnClickListener {
      with(binding) {
        hideKeyboard()
        viewModel.submitUPIDetails(nameEt.text.toString(), upiIdEt.text.toString(), upiIdRepeatEt.text.toString())
      }
    }

    viewModel.uiStateFlow.observe(lifecycle, viewLifecycleScope) { paymentModel -> render(paymentModel) }

    viewModel
      .navigationFlow
      .onEach { paymentDetailNavigation ->
        when (paymentDetailNavigation) {
          PaymentDetailNavigation.VERIFICATION -> navigateToVerification()
          PaymentDetailNavigation.FAILURE -> navigateToFailure()
        }
      }
      .launchIn(viewLifecycleScope)
  }

  private fun render(paymentDetailUPIModel: PaymentDetailUPIModel) {
    if (paymentDetailUPIModel.isLoading) {
      with(binding) {
        progressBar.visible()
        submitButton.gone()
        errorTv.gone()
        errorTv.text = ""
      }
    }

    if (paymentDetailUPIModel.errorMessage.isNotEmpty()) {
      with(binding) {
        progressBar.gone()
        submitButton.visible()
        errorTv.visible()
        errorTv.text = paymentDetailUPIModel.errorMessage
      }
    }
  }

  private fun navigateToVerification() {
    findNavController().navigate(R.id.action_paymentDetailUPIFragment_to_paymentVerificationFragment)
  }

  private fun navigateToFailure() {
    findNavController().navigate(R.id.action_paymentDetailUPIFragment_to_paymentVerificationFragment)
  }

  companion object {
    fun newInstance() = PaymentDetailUPIFragment()
  }
}
