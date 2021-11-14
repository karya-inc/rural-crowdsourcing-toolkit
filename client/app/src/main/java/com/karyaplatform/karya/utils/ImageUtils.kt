// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.karyaplatform.karya.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.karyaplatform.karya.R
import java.io.ByteArrayOutputStream

object ImageUtils {

  /** Profile picture related utilities */

  /** Compress the bitmap output and encode as a base64 string */
  fun bitmapToBase64String(bitmap: Bitmap): String {
    val bufferStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bufferStream)
    return Base64.encodeToString(bufferStream.toByteArray(), Base64.DEFAULT)
  }

  /** Convert base64 string into an image */
  private fun base64StringToBitmap(input: String?): Bitmap? {
    val decodedByte = Base64.decode(input, 0)
    return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
  }

  /** Load image into a image view */
  fun loadImageBitmap(activity: Activity, bitmap: Bitmap, view: ImageView) {
    Glide.with(activity)
      .asBitmap()
      .load(bitmap)
      .apply(
        RequestOptions()
          .format(DecodeFormat.PREFER_ARGB_8888)
          .placeholder(R.drawable.ic_perm_identity)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .circleCrop()
      )
      .into(view)
  }

  /** Load image into a image view */
  fun loadImageString(activity: Activity, base64String: String?, view: ImageView) {
    val bitmap = base64StringToBitmap(base64String)
    Glide.with(activity)
      .asBitmap()
      .load(bitmap)
      .apply(
        RequestOptions()
          .format(DecodeFormat.PREFER_ARGB_8888)
          .placeholder(R.drawable.ic_perm_identity)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .circleCrop()
      )
      .into(view)
  }

  /**
   * Generate bitmap for an image file from its path
   */
  fun bitmapFromFile(path: String): Bitmap {
    val exifInterface = ExifInterface(path)
    val orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)?.toInt() ?: 1
    val rotationMatrix = Matrix()
    rotationMatrix.postRotate(when (orientation) {
      ExifInterface.ORIENTATION_NORMAL -> 0f
      ExifInterface.ORIENTATION_ROTATE_90 -> 90f
      ExifInterface.ORIENTATION_ROTATE_180 -> 180f
      ExifInterface.ORIENTATION_ROTATE_270 -> 270f
      else -> 0f
    })
    val bitmap = BitmapFactory.decodeFile(path)
    val correctedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
    return correctedBitmap
  }
}
