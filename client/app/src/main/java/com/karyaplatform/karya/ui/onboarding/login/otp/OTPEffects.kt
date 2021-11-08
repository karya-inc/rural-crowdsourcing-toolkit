package com.karyaplatform.karya.ui.onboarding.login.otp

import com.karyaplatform.karya.ui.Destination

sealed class OTPEffects {
  data class Navigate(val destination: Destination) : OTPEffects()
}
