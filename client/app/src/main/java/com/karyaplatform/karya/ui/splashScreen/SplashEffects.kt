package com.karyaplatform.karya.ui.splashScreen

sealed class SplashEffects {
  data class UpdateLanguage(val language: String) : SplashEffects()
}
