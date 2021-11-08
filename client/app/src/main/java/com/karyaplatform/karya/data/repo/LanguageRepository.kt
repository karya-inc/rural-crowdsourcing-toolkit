package com.karyaplatform.karya.data.repo

import com.karyaplatform.karya.data.service.LanguageAPI
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LanguageRepository
@Inject
constructor(
  private val languageAPI: LanguageAPI,
) {

  fun getLanguageAssets(accessCode: String, languageCode: String) = flow {
    val response = languageAPI.getLanguageAssets(accessCode, languageCode)

    if (!response.isSuccessful) {
      error("Failed to get file")
    }

    emit(response)
  }
}
