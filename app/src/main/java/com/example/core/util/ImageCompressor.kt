package com.example.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.core.error.AppError
import com.example.core.error.Resource
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Utility for resizing, compressing, and encoding victory/result screenshots
 * into Base64 strings suitable for storage in Firebase Realtime Database
 * under the Spark (free) plan without requiring Cloud Storage or Blaze billing.
 */
object ImageCompressor {

  // Maximum allowed Base64 string payload size in bytes (800 KB)
  const val MAX_PAYLOAD_SIZE_BYTES = 800 * 1024
  const val DEFAULT_MAX_DIMENSION = 1280
  const val INITIAL_JPEG_QUALITY = 80
  const val MIN_JPEG_QUALITY = 35

  const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

  data class CompressionResult(
    val base64String: String,
    val dataUri: String,
    val sizeInBytes: Int,
    val width: Int,
    val height: Int,
  )

  /**
   * Compresses an image from Uri, scales to maintain aspect ratio and legibility of text/usernames,
   * compresses using JPEG, and converts to Base64.
   * Enforces payload size guard (< 800 KB).
   */
  fun compressScreenshot(
    context: Context,
    imageUri: Uri,
    maxDimension: Int = DEFAULT_MAX_DIMENSION,
    maxSizeBytes: Int = MAX_PAYLOAD_SIZE_BYTES,
  ): Resource<CompressionResult> {
    val uriString = imageUri.toString()
    if (uriString.isBlank()) {
      return Resource.Error(AppError.InvalidInput(reason = "স্ক্রিনশট পাথ খালি বা অবৈধ।"))
    }

    val inputStream: InputStream? = try {
      context.contentResolver.openInputStream(imageUri)
    } catch (e: Exception) {
      return Resource.Error(
        AppError.InvalidInput(reason = "স্ক্রিনশট ফাইলটি পড়তে ব্যর্থ হয়েছে: ${e.message ?: "Invalid URI"}")
      )
    }

    if (inputStream == null) {
      return Resource.Error(AppError.InvalidInput(reason = "স্ক্রিনশট পাওয়া যায়নি। দয়া করে আবার চেষ্টা করুন।"))
    }

    return inputStream.use { stream ->
      compressFromStream(stream, maxDimension, maxSizeBytes)
    }
  }

  /**
   * Decodes an InputStream, validates content, downscales, compresses, and returns Base64 data.
   */
  fun compressFromStream(
    stream: InputStream,
    maxDimension: Int = DEFAULT_MAX_DIMENSION,
    maxSizeBytes: Int = MAX_PAYLOAD_SIZE_BYTES,
  ): Resource<CompressionResult> {
    val originalBitmap = try {
      BitmapFactory.decodeStream(stream)
    } catch (e: Exception) {
      null
    }

    if (originalBitmap == null) {
      return Resource.Error(AppError.InvalidInput(reason = "অবৈধ বা ত্রুটিপূর্ণ ছবি। সঠিক স্ক্রিনশট নির্বাচন করুন।"))
    }

    return compressBitmap(originalBitmap, maxDimension, maxSizeBytes)
  }

  /**
   * Resizes and compresses a Bitmap, iteratively adjusting quality to meet maxSizeBytes limit.
   */
  fun compressBitmap(
    bitmap: Bitmap,
    maxDimension: Int = DEFAULT_MAX_DIMENSION,
    maxSizeBytes: Int = MAX_PAYLOAD_SIZE_BYTES,
  ): Resource<CompressionResult> {
    if (bitmap.width <= 0 || bitmap.height <= 0) {
      return Resource.Error(AppError.InvalidInput(reason = "অবৈধ ছবির মাপ। সঠিক ছবি নির্বাচন করুন।"))
    }

    // Step 1: Scale down if larger than maxDimension while preserving aspect ratio
    val width = bitmap.width
    val height = bitmap.height
    val scale = if (width > maxDimension || height > maxDimension) {
      if (width > height) maxDimension.toFloat() / width else maxDimension.toFloat() / height
    } else {
      1.0f
    }

    val scaledBitmap = if (scale < 1.0f) {
      val targetW = (width * scale).toInt().coerceAtLeast(1)
      val targetH = (height * scale).toInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    } else {
      bitmap
    }

    // Step 2: Progressively compress to find suitable quality below maxSizeBytes
    var currentQuality = INITIAL_JPEG_QUALITY
    var chosenBytes: ByteArray? = null
    var chosenBase64: String? = null

    while (currentQuality >= MIN_JPEG_QUALITY) {
      val baos = ByteArrayOutputStream()
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos)
      val bytes = baos.toByteArray()
      val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      val totalPayloadSize = DATA_URI_PREFIX.length + base64.length

      if (totalPayloadSize <= maxSizeBytes) {
        chosenBytes = bytes
        chosenBase64 = base64
        break
      }

      currentQuality -= 10
    }

    // Step 3: If still oversized after quality reduction, perform secondary downscale
    if (chosenBase64 == null) {
      val secondaryScale = 0.65f
      val secondScaledBitmap = Bitmap.createScaledBitmap(
        scaledBitmap,
        (scaledBitmap.width * secondaryScale).toInt().coerceAtLeast(1),
        (scaledBitmap.height * secondaryScale).toInt().coerceAtLeast(1),
        true
      )
      val baos = ByteArrayOutputStream()
      secondScaledBitmap.compress(Bitmap.CompressFormat.JPEG, MIN_JPEG_QUALITY, baos)
      val bytes = baos.toByteArray()
      val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
      val totalPayloadSize = DATA_URI_PREFIX.length + base64.length

      if (totalPayloadSize <= maxSizeBytes) {
        chosenBytes = bytes
        chosenBase64 = base64
      }
    }

    if (chosenBase64 == null || chosenBytes == null) {
      return Resource.Error(
        AppError.InvalidInput(
          reason = "স্ক্রিনশটের সাইজ অতিরিক্ত বড়। নিরাপদ সীমা (${maxSizeBytes / 1024} KB) এর মধ্যে রাখা সম্ভব হয়নি। অনুগ্রহ করে ছোট বা পরিষ্কার ছবি দিন।"
        )
      )
    }

    val fullDataUri = DATA_URI_PREFIX + chosenBase64
    val payloadSize = fullDataUri.toByteArray().size

    if (payloadSize > maxSizeBytes) {
      return Resource.Error(
        AppError.InvalidInput(
          reason = "স্ক্রিনশটের সাইজ অতিরিক্ত বড়। নিরাপদ সীমা (${maxSizeBytes / 1024} KB) এর মধ্যে রাখা সম্ভব হয়নি। অনুগ্রহ করে ছোট বা পরিষ্কার ছবি দিন।"
        )
      )
    }

    return Resource.Success(
      CompressionResult(
        base64String = chosenBase64,
        dataUri = fullDataUri,
        sizeInBytes = payloadSize,
        width = scaledBitmap.width,
        height = scaledBitmap.height,
      )
    )
  }

  /**
   * Helper to decode a Base64 string or Data URI back to bytes for verification.
   */
  fun decodeBase64(base64OrDataUri: String): ByteArray {
    val cleanBase64 = if (base64OrDataUri.startsWith(DATA_URI_PREFIX)) {
      base64OrDataUri.removePrefix(DATA_URI_PREFIX)
    } else if (base64OrDataUri.contains(",")) {
      base64OrDataUri.substringAfter(",")
    } else {
      base64OrDataUri
    }
    return Base64.decode(cleanBase64, Base64.DEFAULT)
  }
}
