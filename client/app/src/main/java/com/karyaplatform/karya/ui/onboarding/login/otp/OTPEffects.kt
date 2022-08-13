package com.karyaplatform.karya.ui.onboarding.login.otp

import com.karyaplatform.karya.ui.Destination

sealed class OTPEffects {
  object NavigateToProfile : OTPEffects()
  object NavigateToHomeScreen: OTPEffects()
}
