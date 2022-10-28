package com.karyaplatform.karya.ui.payment.registration

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentPaymentRegistrationBinding
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.viewLifecycle
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentRegistrationFragment : Fragment(R.layout.fragment_payment_registration) {

  private val binding by viewBinding(FragmentPaymentRegistrationBinding::bind)
  private val viewModel by viewModels<PaymentRegistrationViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupListeners()
  }

  private fun setupListeners() {
    binding.backAccountCv.setOnClickListener {
      viewModel.selectPaymentMethod(PaymentMethod.BANK_ACCOUNT)
      navigateToDetails(PaymentMethod.BANK_ACCOUNT)
    }

    binding.upiIdCv.setOnClickListener {
      viewModel.selectPaymentMethod(PaymentMethod.UPI)
      navigateToDetails(PaymentMethod.UPI)
    }

    viewModel.uiStateFlow.observe(viewLifecycle, viewLifecycleScope) { paymentModel -> render(paymentModel) }
  }

  private fun render(paymentRegistrationModel: PaymentRegistrationModel) {
    when (paymentRegistrationModel.selection) {
      PaymentMethod.NONE -> {
        binding.bankIv.isSelected = false
        binding.upiIv.isSelected = false
      }
      PaymentMethod.BANK_ACCOUNT -> {
        binding.bankIv.isSelected = true
        binding.upiIv.isSelected = false
      }
      PaymentMethod.UPI -> {
        binding.bankIv.isSelected = false
        binding.upiIv.isSelected = true
      }
    }

    binding.balanceAmountTv.text = getString(R.string.rs_float, paymentRegistrationModel.amountEarned)
  }

  private fun navigateToDetails(paymentMethod: PaymentMethod) {
    val action =
      when (paymentMethod) {
        PaymentMethod.NONE -> return
        PaymentMethod.BANK_ACCOUNT -> R.id.action_paymentRegistrationFragment_to_paymentDetailBankFragment
        PaymentMethod.UPI -> R.id.action_paymentRegistrationFragment_to_paymentDetailUPIFragment
      }

    findNavController().navigate(action)
  }

  companion object {
    fun newInstance() = PaymentRegistrationFragment()
  }
}
