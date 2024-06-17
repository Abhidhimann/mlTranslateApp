package com.example.tempapplication.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.Toast
import java.io.File


object CommonUtils {

    private var currentToast: Toast? = null
    // Toast can outlive activity
    fun shortToast(context: Context, text: String) {
        if (currentToast != null) return
        currentToast = Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT)
        currentToast?.show()
        Handler(Looper.getMainLooper()).postDelayed({ currentToast = null }, 3000)
    }

    fun longToast(context: Context, text: String) {
        if (currentToast != null) return
        currentToast = Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG)
        currentToast?.show()
        Handler(Looper.getMainLooper()).postDelayed({ currentToast = null }, 5000)
    }

    fun getTempFile(activity: Activity, suffix: String): File {
        val imageFileName = "${System.currentTimeMillis()}"
        val tempFile =
            File.createTempFile(imageFileName, suffix, activity.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
        return tempFile
    }

    fun dpToPx(context: Context, valueInDp: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
    }

    class TranslationFailedException(message: String) : Exception(message)
}