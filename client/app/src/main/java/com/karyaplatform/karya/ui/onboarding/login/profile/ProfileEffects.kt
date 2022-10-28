package com.karyaplatform.karya.ui.onboarding.login.profile

import com.karyaplatform.karya.ui.Destination

sealed class ProfileEffects {
  data class Navigate(val destination: Destination) : ProfileEffects()
}
