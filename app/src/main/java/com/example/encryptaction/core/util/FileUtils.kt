package com.example.encryptaction.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies a content URI to a temp file in the app's cache directory.
 * Runs on Dispatchers.IO — safe for Google Drive URIs that trigger a network download.
 * Returns the temp file, or null on failure.
 * The caller is responsible for deleting the file when done.
 */
suspend fun Uri.toTempFile(context: Context): File? = withContext(Dispatchers.IO) {
    try {
        val fileName = resolveDisplayName(context) ?: "temp_${System.currentTimeMillis()}"
        val tempFile = File(context.cacheDir, "pick_$fileName")
        val stream = context.contentResolver.openInputStream(this@toTempFile)
            ?: return@withContext null
        stream.use { input -> tempFile.outputStream().use { input.copyTo(it) } }
        tempFile
    } catch (e: Throwable) {
        null
    }
}

/**
 * Resolves the display name of a content URI.
 * Safe for all providers including Google Drive — exceptions are caught and fallback to
 * the last path segment is used.
 */
fun Uri.resolveDisplayName(context: Context): String? {
    return try {
        var name: String? = null
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
        name ?: lastPathSegment
    } catch (e: Throwable) {
        lastPathSegment
    }
}

fun Uri.resolveMimeType(context: Context): String =
    context.contentResolver.getType(this) ?: "application/octet-stream"
