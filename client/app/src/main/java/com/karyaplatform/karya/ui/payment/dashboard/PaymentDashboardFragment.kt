package com.karyaplatform.karya.ui.payment.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.karyaplatform.karya.R
import com.karyaplatform.karya.databinding.FragmentPaymentDashboardBinding
import com.karyaplatform.karya.utils.extensions.gone
import com.karyaplatform.karya.utils.extensions.observe
import com.karyaplatform.karya.utils.extensions.viewBinding
import com.karyaplatform.karya.utils.extensions.viewLifecycle
import com.karyaplatform.karya.utils.extensions.viewLifecycleScope
import com.karyaplatform.karya.utils.extensions.visible
import com.karyaplatform.karya.utils.extensions.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.app_toolbar.view.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class PaymentDashboardFragment : Fragment(R.layout.fragment_payment_dashboard) {
  private val binding by viewBinding(FragmentPaymentDashboardBinding::bind)
  private val viewModel by viewModels<PaymentDashboardViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupListeners()
    viewModel.fetchData()
  }

  private fun setupListeners() {
    binding.appTb.toolbarBackBtn.visible()
    binding.appTb.toolbarBackBtn.setOnClickListener {
      requireActivity().onBackPressed()
    }

    viewModel
      .navigationFlow
      .onEach { paymentDashboardNavigation -> navigate(paymentDashboardNavigation) }
      .launchIn(viewLifecycleScope)

    viewModel.uiStateFlow.observe(viewLifecycle, viewLifecycleScope) { render(it) }

    binding.transactionsButton.setOnClickListener { navigate(PaymentDashboardNavigation.TRANSACTIONS) }

    binding.updateAccountButton.setOnClickListener { navigate(PaymentDashboardNavigation.REGISTRATION) }
  }

  private fun render(paymentDashboardModel: PaymentDashboardModel) {
    if (paymentDashboardModel.isLoading) {
      showLoading()
    } else {
      with(binding) {
        balanceAmountTv.text = getString(R.string.rs_float, paymentDashboardModel.balance)
        transferredAmountTv.text = getString(R.string.rs_float, paymentDashboardModel.transferred)
        setupAccountCard(paymentDashboardModel.userAccountDetail)
        setupTransactionCard(paymentDashboardModel.userTransactionDetail)
      }
      hideLoading()
    }
  }

  private fun setupAccountCard(userAccountDetail: UserAccountDetail) {
    with(binding) {
      nameTv.text = userAccountDetail.name
      accountIdTv.text = userAccountDetail.id
    }
  }

  private fun setupTransactionCard(userTransactionDetail: UserTransactionDetail) {
    with(binding.latestTransactionCv) {
      transactionValueTv.text = getString(R.string.rs_float, userTransactionDetail.amount)
      referenceTv.text = userTransactionDetail.utr
      dateTv.text = userTransactionDetail.date

      transactionSuccessIv.invisible()
      transactionFailureIv.invisible()
      transactionPendingIv.invisible()

      when (userTransactionDetail.status) {
        "processed" -> transactionSuccessIv
        "reversed" -> transactionFailureIv
        else -> transactionPendingIv
      }.visible()
    }
  }

  private fun showLoading() {
    with(binding) {
      paymentsLl.invisible()
      progressBar.visible()
    }
  }

  private fun hideLoading() {
    with(binding) {
      paymentsLl.visible()
      progressBar.invisible()
    }
  }

  private fun navigate(paymentDashboardNavigation: PaymentDashboardNavigation) {
    val resId =
      when (paymentDashboardNavigation) {
        PaymentDashboardNavigation.TRANSACTIONS -> R.id.action_paymentDashboardFragment_to_paymentTransactionFragment
        PaymentDashboardNavigation.REGISTRATION -> R.id.action_paymentDashboardFragment_to_paymentRegistrationFragment
      }
    findNavController().navigate(resId)
  }
}
