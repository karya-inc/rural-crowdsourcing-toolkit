package com.karyaplatform.karya.ui.payment.verification

data class PaymentVerificationModel(
  val isLoading: Boolean,
  val requestProcessed: Boolean,
  val errorMessage: String = "",
)
