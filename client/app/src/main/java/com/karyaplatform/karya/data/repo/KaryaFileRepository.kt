package com.karyaplatform.karya.data.repo

import com.karyaplatform.karya.data.local.daos.KaryaFileDao
import com.karyaplatform.karya.data.model.karya.KaryaFileRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class KaryaFileRepository
@Inject
constructor(private val karyaFileDao: KaryaFileDao) {
  suspend fun insertKaryaFile(karyaFileRecord: KaryaFileRecord) {
    withContext(Dispatchers.IO) {
      try {
        karyaFileDao.insert(karyaFileRecord)
      } catch (e: Exception) {
        karyaFileDao.upsert(karyaFileRecord)
      }
    }
  }
}
