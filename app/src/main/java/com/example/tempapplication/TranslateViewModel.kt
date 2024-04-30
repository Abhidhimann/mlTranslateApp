package com.example.tempapplication

import android.util.Log
import android.util.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tempapplication.utils.AvailLanguages
import com.example.tempapplication.utils.tempTag
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/*
 * https://github.com/googlesamples/mlkit/blob/master/android/translate/app/src/main/java/com/google/mlkit/samples/nl/translate/kotlin/TranslateViewModel.kt
 *
 * Used LruCache that's why putting reference
 */

class TranslateViewModel : ViewModel() {

    companion object {
        // This specifies the number of translators instance we want to keep in our LRU cache.
        private const val MAX_NUM_INSTANCE = 3

        // This specifies the number of model after we will show some warning to user
        private const val MAX_NUM_MODELS_TO_SHOW_WARNING = 5
    }


    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    private val _availableModels = mutableListOf<String>()
    val availableModels: List<String> = _availableModels

        /* pair of (languageCode, Downloading Task)
         * need this coz download is big 30 mb, user can download in background also
         * and if stops same model downloading
         */
    private val pendingDownloads: HashMap<String, Task<Void>> = hashMapOf()

    private val _translateErrorState = MutableLiveData(false)
    val translateErrorState: LiveData<Boolean> = _translateErrorState

    private val _downloadErrorState = MutableLiveData(false)
    val downloadErrorState: LiveData<Boolean> = _downloadErrorState

    private val _translatedText = MutableLiveData<String>()
    val translatedText: LiveData<String> = _translatedText

    private val _newModelDownloadProgress = MutableLiveData(false)
    val newModelDownloadProgress: LiveData<Boolean> = _newModelDownloadProgress

    private val _identifiedLang = MutableLiveData("und")
    val identifiedLang: LiveData<String> = _identifiedLang

    var langDetectionState = true

    // just for helping, it was irritating to find every time in enum values
    val availLanguagesCodeToValueMap: Map<String, String> =
        AvailLanguages.values().associate { it.code to it.value }
    val availLanguagesValueToCodeMap =
        availLanguagesCodeToValueMap.entries.associate { (k, v) -> v to k }

    // a key value pair to manage language models
    private val translators =
        object : LruCache<TranslatorOptions, Translator>(MAX_NUM_INSTANCE) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?,
            ) {
                oldValue.close()
            }
        }

    init {
        viewModelScope.launch {
            updateAvailableModels()
        }
    }

    private suspend fun updateAvailableModels() {
        withContext(Dispatchers.IO) {
            try {
                val remoteModels =
                    modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
                val languages = remoteModels.map { it.language }
                _availableModels.clear()
                _availableModels.addAll(languages)
                Log.i(tempTag(), "available models are2 $_availableModels")
            } catch (e: Exception) {
                Log.i(tempTag(), "Error while retrieving models. Reason -> $e")
            }
        }
    }

    fun initializeTranslationState() {
        _downloadErrorState.postValue(false)
        _translateErrorState.postValue(false)
//        _translatedText.postValue("")
    }

    private fun findModelByCode(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }

    private suspend fun deleteModel(languageCode: String) = withContext(Dispatchers.IO) {
        val model = findModelByCode(languageCode)
        try {
            modelManager.deleteDownloadedModel(model).await()
            updateAvailableModels()
        } catch (e: Exception) {
            Log.i(tempTag(), "Error while deleting $languageCode model")
            throw TranslationFailedException(e.toString())
        }

    }

    private suspend fun downloadModel(languageCode: String) {
        withContext(Dispatchers.IO) {
            if (_availableModels.contains(languageCode)) {
                Log.i(tempTag(), "model $languageCode is already present")
                return@withContext
            }

            var downloadTask: Task<Void>?

            // If model is in process of downloading
            if (pendingDownloads.containsKey(languageCode)) {
                downloadTask = pendingDownloads[languageCode]
                // found existing downloading task
                Log.i(tempTag(), "Model $languageCode downloading is in process please wait")
                if (downloadTask != null && !downloadTask.isCanceled) {
                    return@withContext
                }
            }

            val model = findModelByCode(languageCode)
            val downloadConditions = DownloadConditions.Builder().build() // can give wifi also

            try {
                Log.i(tempTag(), "Model $languageCode is not present so starting downloading")
                downloadTask = modelManager.download(model, downloadConditions)
                pendingDownloads[languageCode] = downloadTask
                downloadTask.await()
                Log.i(tempTag(), "Model $languageCode downloaded successfully from internet")
                updateAvailableModels()
                return@withContext
            } catch (e: Exception) {
                Log.i(tempTag(), "error while downloading model $languageCode. Reason -> $e")
                throw TranslationFailedException(e.toString())
            }
        }
    }


    private suspend fun translatorInitialization(options: TranslatorOptions) =
        withContext(Dispatchers.IO) {
            try {
                translators[options].downloadModelIfNeeded().await()
                Log.i(tempTag(), "Model is active to start the translation")
            } catch (e: Exception) {
                Log.i(tempTag(), "Some error occurred -> $e")
                throw TranslationFailedException(e.toString())
            }
        }

    private suspend fun translateText(sourceText: String, options: TranslatorOptions): String =
        withContext(Dispatchers.IO) {
            try {
                val translatedText = translators[options].translate(sourceText).await()
                Log.i(
                    tempTag(),
                    "Translation done successfully: $sourceText -> $translatedText"
                )
                return@withContext translatedText
            } catch (e: Exception) {
                Log.i(tempTag(), "Some error occurred while translating. Reason -> $e")
                throw TranslationFailedException(e.toString())
            }
        }

    fun identifyLanguage(text: String) = viewModelScope.launch {
        val identifiedLanguage = identifyLanguageCode(text)
        _identifiedLang.postValue(identifiedLanguage)
    }

    private suspend fun identifyLanguageCode(text: String): String {
        return withContext(Dispatchers.IO) {
            Log.i(tempTag(), "Passed text to get language code is $text")
            try {
                val languageIdentifier = LanguageIdentification.getClient()
                val languageCode = languageIdentifier.identifyLanguage(text).await().toString()
                Log.i(tempTag(), "identified language is: $languageCode")
                return@withContext languageCode
            } catch (e: Exception) {
                Log.i(tempTag(), "Language identification failed. Reason -> $e")
                return@withContext AvailLanguages.DETECT_LANG.code // unIdentified language
            }
        }
    }

    fun downloadNewModels(sourceLangCode: String, targetLanguageCode: String) =
        viewModelScope.launch {
            _newModelDownloadProgress.postValue(true)
            val downloadJobs = listOf(
                launch {
                    try {
                        downloadModel(targetLanguageCode)
                    } catch (e: Exception) {
                        Log.i(
                            tempTag(),
                            "Error occurred while downloading target language model: $e"
                        )
                        pendingDownloads.clear()
                        _downloadErrorState.postValue(true)
                    }
                },
                launch {
                    try {
                        downloadModel(sourceLangCode)
                    } catch (e: Exception) {
                        Log.i(
                            tempTag(),
                            "Error occurred while downloading source language model: $e"
                        )
                        pendingDownloads.clear()
                        _downloadErrorState.postValue(true)
                    }
                }
            )
            downloadJobs.joinAll()
            _newModelDownloadProgress.postValue(false)
        }

    fun myTranslate(sourceText: String, sourceLangCode: String, targetLangCode: String) =
        viewModelScope.launch {
            try {
                initializeTranslationState()
                val options =
                    TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLangCode)
                        .setTargetLanguage(targetLangCode)
                        .build()
                translatorInitialization(options)
                val translatedText = translateText(sourceText, options)
                if (translatedText.isNullOrEmpty()) {
                    _translateErrorState.postValue(true)
                } else {
                    _translatedText.postValue(translatedText)
                }
            } catch (e: Exception) {
                Log.i(tempTag(), "Error occurred -> $e")
                _translateErrorState.postValue(true)
            }
        }

    private suspend fun checkIfNecessaryModelsPresent(
        sourceLangCode: String,
        targetLanguageCode: String
    ): MutableList<String> =
        viewModelScope.async {
            updateAvailableModels()
            val langCodes = mutableListOf<String>()
            if (!_availableModels.contains(sourceLangCode)) {
                langCodes.add(sourceLangCode)
            }
            if (!_availableModels.contains(targetLanguageCode)) {
                langCodes.add(targetLanguageCode)
            }
            return@async langCodes
        }.await()


    fun checkIfModelIsPresent(
        sourceLangCode: String,
        targetLanguageCode: String,
        callback: (List<String>) -> Unit
    ) =
        viewModelScope.launch {
            callback(checkIfNecessaryModelsPresent(sourceLangCode, targetLanguageCode))
        }

    private fun deleteLanguageModel(languageCode: String) {
        viewModelScope.launch {
            try {
                deleteModel(languageCode)
            } catch (e: Exception) {
                Log.i(tempTag(), "Error occurred while deleting -> $e")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        translators.evictAll()
    }
}

class TranslationFailedException(message: String) : Exception(message)