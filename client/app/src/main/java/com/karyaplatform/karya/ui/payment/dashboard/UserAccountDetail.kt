package com.karyaplatform.karya.ui.payment.dashboard

data class UserAccountDetail(
  val name: String,
  val id: String,
  val accountType: String,
  // TODO: handle vpa
  val ifsc: String
)
