package com.example.tempapplication.utils

class Tags

fun Any.classTag(): String = this::class.java.simpleName

fun Any.tempTag(): String = "tempTag"

const val STORAGE_PERMISSION_REQUEST_CODE = 20
const val PERMISSIONS_REQUEST_RECORD_AUDIO = 10
const val RESULTS_LIMIT = 1
const val REQUEST_CAMERA_PERMISSION = 30
const val IS_CONTINUES_LISTEN = false
const val IMAGE_URI = "ImageURI"
const val CACHE_IMAGE_FOLDER = "images"