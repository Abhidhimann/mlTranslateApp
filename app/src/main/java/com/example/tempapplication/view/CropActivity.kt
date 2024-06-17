package com.example.tempapplication.view

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.tempapplication.BuildConfig
import com.example.tempapplication.databinding.ActivityCropBinding
import com.example.tempapplication.utils.CACHE_IMAGE_FOLDER
import java.io.File
import java.io.FileOutputStream

class CropActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private lateinit var binding: ActivityCropBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // use variable
        val uri = intent.getStringExtra("ImageURI")
        if (!uri.isNullOrEmpty()){
            imageUri= Uri.parse(uri)
            binding.cropImageView.setImageUriAsync(imageUri)
        }
        cropCancelListener()
        resetImageCropListener()
        rotateImageListener()
        cropImageListener()
    }

    private fun cropCancelListener(){
        binding.cancelCrop.setOnClickListener {
            val resultIntent = Intent()
            setResult(RESULT_CANCELED, resultIntent)
            finish()
        }
    }

    private fun resetImageCropListener(){
        binding.resetCrop.setOnClickListener {
            binding.cropImageView.resetCropRect()
        }
    }

    private fun rotateImageListener(){
        binding.rotateImage.setOnClickListener {
            binding.cropImageView.rotateImage(90)
        }
    }

    private fun cropImageListener(){
        binding.cropImage.setOnClickListener {
            val resultImage = binding.cropImageView.getCroppedImage()
            returnImageResult(resultImage)
        }
    }

    private fun returnImageResult(bitmap: Bitmap?) {
        val cachePath = File(cacheDir, CACHE_IMAGE_FOLDER)
        cachePath.mkdirs() // Make sure the directory exists
        val imageFile = File(cachePath, "${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(imageFile).use { out ->
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val imageUri =
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", imageFile)
        val resultIntent = Intent()
        resultIntent.setData(imageUri)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}