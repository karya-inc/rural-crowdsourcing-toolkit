package com.karyaplatform.karya.ui.payment.transactions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentPaymentTransactionBinding
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.viewLifecycle
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentTransactionFragment : Fragment(R.layout.fragment_payment_transaction) {
  private val binding by viewBinding(FragmentPaymentTransactionBinding::bind)
  private val viewModel by activityViewModels<PaymentTransactionViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupListeners()
    binding.transactionRv.adapter = PaymentTransactionAdapter()
    viewModel.fetchTransactions()
  }

  private fun setupListeners() {
    viewModel.uiStateFlow.observe(viewLifecycle, viewLifecycleScope) {
      println(it)
      render(it)
    }
  }

  private fun render(paymentTransactionModel: PaymentTransactionModel) {
    if (paymentTransactionModel.isLoading) {
      binding.progressBar.visible()
      binding.transactionRv.gone()
      return
    }

    if (paymentTransactionModel.userTransactionDetailList.isNotEmpty()) {
      val adapter = binding.transactionRv.adapter
      if (adapter is PaymentTransactionAdapter) {
        adapter.updateList(paymentTransactionModel.userTransactionDetailList)
      }
      binding.progressBar.gone()
      binding.transactionRv.visible()
    }
  }
}
