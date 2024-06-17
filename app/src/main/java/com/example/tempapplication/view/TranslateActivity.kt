package com.example.tempapplication.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.example.tempapplication.BuildConfig
import com.example.tempapplication.R
import com.example.tempapplication.TranslateViewModel
import com.example.tempapplication.databinding.ActivityMainBinding
import com.example.tempapplication.utils.*
import com.example.tempapplication.utils.CommonUtils.getTempFile
import com.example.tempapplication.utils.CommonUtils.shortToast
import com.example.tempapplication.utils.DialogUtils.dialogSearchAbleSpinnerInit
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.util.*


// TODO
// layout change when keyboard
// maybe a dialog when user Permanently denies permission

class TranslateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fromLangSpinnerAdapter: ArrayAdapter<String>
    private lateinit var toLangSpinnerAdapter: ArrayAdapter<String>
    private lateinit var textToSpeechSourceText: TextToSpeech
    private lateinit var textToSpeechResultText: TextToSpeech
    private lateinit var fromLangCustomDialog: Dialog
    private lateinit var selectImageCustomDialog: BottomSheetDialog
    private lateinit var toLangCustomDialog: Dialog
    private lateinit var downloadedModelDialog: Dialog
    private lateinit var popupMenu: PopupMenu
    private lateinit var modelDownloadDialog: Dialog
    private var doNotShowAlertBoxFlag = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private lateinit var translateViewModel: TranslateViewModel
    private var makeTranslationDisabled = false
    private var makeSourceTextTalkDisabled = false
    private var makeResultTextTalkDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        translateViewModel =
            ViewModelProvider(this)[TranslateViewModel::class.java]

        initialization()
        spinnerListeners()
        textChangeListeners()
        textOperationsIconsListeners()
        selectImageListener()
        threeDotMenuClickListener()
        translationObservers()
        binding.sourceText.setOnClickListener {
            Log.d(tempTag(), "click happened")
        }
        binding.sourceText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // User clicked on EditText and it gained focus
                Log.d(tempTag(), "has focus")
            } else {
                Log.d(tempTag(), "lost focus")
            }
        }
    }

    private fun initialization() {
        languageSpinnerInit()
        textOperationsIconsVisibilityInit()
        textToSpeechInit()
        fromLangDialogSearchAbleSpinnerInit()
        toLangDialogSearchAbleSpinnerInit()
        downloadedModelDialogInit()
        modelDownloadProgressDialogInit()
        initSpeechRecognizer()
        textContainerInit()
        popUpMenuInit()
        bottomImageSelectDialogInit()
    }

    private fun spinnerListeners() {
        interchangeLangClickListener()
        fromLangSpinnerTouchListener()
        toLangSpinnerTouchListener()
        formLanguageSpinnerItemChangeListener()
        toLanguageSpinnerItemChangeListener()
    }

    private fun textChangeListeners() {
        sourceTextChangeListener()
        resultTextChangeListener()
    }


    private fun translationObservers() {
        languageIdentifierObserver()
        translationErrorObserver()
        translationModelDownloadErrorObserver()
        translatedTextObserver()
        translateModelDownloadObserver()
        ocrResultObserver()
    }

    private fun textOperationsIconsListeners() {
        sourceTextTalkIconListener()
        sourceTextCopyIconListener()
        sourceTextClearIconListener()
        resultTextTalkIconListener()
        resultTextCopyIconListener()
        resultTextClearIconListener()
        sourceTextMicListener()
        sourceTextSpeechStopListener()
    }

    private fun popUpMenuInit() {
        popupMenu = PopupMenu(this@TranslateActivity, binding.toolbar.threeDotMenu)
        popupMenu.menuInflater.inflate(R.menu.menu_items, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.models -> {
                    downloadedModelDialog.show()
                    true
                }

                else -> false
            }
        }
    }

    private fun threeDotMenuClickListener(){
        binding.toolbar.threeDotMenu.setOnClickListener {
            popupMenu.show()
        }
    }

    private fun selectImageListener() {
        binding.selectImage.setOnClickListener {
            if (binding.FromLang.selectedItem == SupportedLanguages.DETECT_LANG.value) {
                shortToast(this, getString(R.string.select_language))
                return@setOnClickListener
            }
            selectImageCustomDialog.show()
        }
    }

    private fun bottomImageSelectDialogInit() {
        selectImageCustomDialog = BottomSheetDialog(this)
        selectImageCustomDialog.setContentView(R.layout.select_image_dailog_layout)
        selectImageCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val galleryView: LinearLayout? = selectImageCustomDialog.findViewById(R.id.sourceGallery)
        val cameraView: LinearLayout? = selectImageCustomDialog.findViewById(R.id.sourceCamera)
        val cancelView: ImageView? = selectImageCustomDialog.findViewById(R.id.close_dialog)
        galleryView?.setOnClickListener {
            if (requestStoragePermissionIfNot(applicationContext)) {
                getImageFromGallery.launch("image/*")
                selectImageCustomDialog.dismiss()
            }
        }

        cameraView?.setOnClickListener {
            if (requestCameraPermissionIfNot(applicationContext)) {
                launchCamera()
                selectImageCustomDialog.dismiss()
            }
        }

        cancelView?.setOnClickListener {
            selectImageCustomDialog.dismiss()
        }
    }

    private fun sourceTextMicListener() {
        binding.sourceTextMic.setOnClickListener {
            if (binding.FromLang.selectedItem == SupportedLanguages.DETECT_LANG.value) {
                shortToast(this, getString(R.string.select_language))
                return@setOnClickListener
            }
            if (requestVoiceRecordPermissionIfNot(applicationContext)) {
                startListening()
            }
        }
    }

    private fun sourceTextSpeechStopListener() {
        binding.sourceTextListenStop.setOnClickListener {
            speechRecognizer!!.stopListening()
            binding.sourceTextListenStop.visibility = View.GONE
            binding.sourceTextMic.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun fromLangSpinnerTouchListener() {
        binding.FromLang.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    fromLangCustomDialog.show()
                    true
                }

                else -> {
                    true
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toLangSpinnerTouchListener() {
        binding.ToLang.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    toLangCustomDialog.show()
                    true
                }

                else -> {
                    true
                }
            }
        }
    }

    // can do by textView also will be very easy and clear, because previous I used spinner so...
    private fun fromLangDialogSearchAbleSpinnerInit() {
        fromLangCustomDialog = dialogSearchAbleSpinnerInit(
            this@TranslateActivity,
            resources.displayMetrics,
            SupportedLanguages.values().toList(),
            translateViewModel.availableModels
        ) {
            binding.FromLang.setSelection(
                fromLangSpinnerAdapter.getPosition(
                    it.value
                )
            )
            fromLangCustomDialog.dismiss()
        }
    }

    private fun toLangDialogSearchAbleSpinnerInit() {
        toLangCustomDialog = dialogSearchAbleSpinnerInit(
            this,
            resources.displayMetrics,
            SupportedLanguages.values().filter { it != SupportedLanguages.DETECT_LANG },
            translateViewModel.availableModels
        ) {
            binding.ToLang.setSelection(
                toLangSpinnerAdapter.getPosition(
                    it.value
                )
            )
            toLangCustomDialog.dismiss()
        }
    }

    private fun downloadedModelDialogInit() {
        downloadedModelDialog = Dialog(this)
        downloadedModelDialog.setContentView(R.layout.downloaded_model_dialog_layout)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        downloadedModelDialog.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (screenHeight * 0.6).toInt()
        )
        downloadedModelDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val listView: ListView = downloadedModelDialog.findViewById(R.id.model_list_view)

        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            this@TranslateActivity,
            R.layout.downloaded_model_view,
            translateViewModel.availableModels
        ) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var listItemView = convertView
                if (listItemView == null) {
                    listItemView = LayoutInflater.from(context)
                        .inflate(R.layout.downloaded_model_view, parent, false)
                }

                val modelCode = getItem(position) ?: "Undefined"
                val modelTextView = listItemView!!.findViewById<TextView>(R.id.modelName)
                val modelDelete = listItemView.findViewById<ImageView>(R.id.deleteModel)

                val modelName =
                    translateViewModel.supportedLanguagesCodeToValueMap[modelCode] ?: "Undefined"
                modelTextView.text = modelName
                modelDelete.setOnClickListener {
                    downloadedModelDialog.dismiss()
                    simpleDialog(modelName, modelCode)
                }
                return listItemView
            }
        }
        listView.adapter = adapter
    }

    private fun simpleDialog(modelName: String, modelCode: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(getString(R.string.delete_model_confirmation, modelName))
            .setPositiveButton("Yes") { _, _ ->
                translateViewModel.deleteLanguageModel(modelCode)
            }
            .setNegativeButton("No", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    private fun languageSpinnerInit() {
        fromLangSpinnerAdapter = ArrayAdapter(
            this,
            R.layout.custom_spinner_item,
            translateViewModel.supportedLanguagesCodeToValueMap.values.toList()
        )
        binding.FromLang.adapter = fromLangSpinnerAdapter
        setLanguageInFromLanguageSpinner(SupportedLanguages.DETECT_LANG.value)

        toLangSpinnerAdapter = ArrayAdapter(
            this,
            R.layout.custom_spinner_item,
            translateViewModel.supportedLanguagesCodeToValueMap.values.filter { it != SupportedLanguages.DETECT_LANG.value }
                .toList()
        )
        binding.ToLang.adapter = toLangSpinnerAdapter
        setLanguageInToLanguageSpinner(SupportedLanguages.ENGLISH.value)
    }

    private fun textOperationsIconsVisibilityInit() {
        binding.resultTextTalk.visibility = View.GONE
        binding.resultTextCopy.visibility = View.GONE
        binding.resultTextClear.visibility = View.GONE
        binding.sourceTextTalk.visibility = View.GONE
        binding.sourceTextCopy.visibility = View.GONE
        binding.sourceTextClear.visibility = View.GONE
    }

    private fun interchangeLangClickListener() {
        binding.interchangeLang.setOnClickListener {
            val temp = binding.FromLang.selectedItem.toString()
            binding.FromLang.setSelection(fromLangSpinnerAdapter.getPosition(binding.ToLang.selectedItem.toString()))
            if (temp == SupportedLanguages.DETECT_LANG.value) {
                binding.ToLang.setSelection(toLangSpinnerAdapter.getPosition(SupportedLanguages.ENGLISH.value))
            } else {
                binding.ToLang.setSelection(toLangSpinnerAdapter.getPosition(temp))
            }
        }
    }

    private fun languageIdentifierObserver() {
        translateViewModel.identifiedLang.observe(this) { langCode ->
            if (binding.FromLang.selectedItem != SupportedLanguages.DETECT_LANG.value) {
                return@observe
            }
            if (langCode == null || langCode == SupportedLanguages.DETECT_LANG.code) {
                setLanguageInFromLanguageSpinner(SupportedLanguages.DETECT_LANG.value)
                return@observe
            }
            val language = translateViewModel.supportedLanguagesCodeToValueMap[langCode.lowercase()]
            Log.i(classTag(), "identified lang $language")
            if (language != null) {
                setLanguageInFromLanguageSpinner(language)
            } else {
                makeTranslationDisabled = true
            }
        }
    }


    private fun setLanguageInFromLanguageSpinner(text: String) {
        binding.FromLang.setSelection(fromLangSpinnerAdapter.getPosition(text))
    }

    private fun setLanguageInToLanguageSpinner(text: String) {
        binding.ToLang.setSelection(toLangSpinnerAdapter.getPosition(text))
    }

    private fun formLanguageSpinnerItemChangeListener() {
        binding.FromLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                translateViewModel.langDetectionState =
                    selectedItem == SupportedLanguages.DETECT_LANG.value
                if (translateViewModel.langDetectionState && binding.sourceText.text.length > 4) {
                    translateViewModel.identifyLanguage(binding.sourceText.text.toString())
                } else {
                    setLanguageForSourceTextToSpeech(selectedItem)
                }

                if (selectedItem != SupportedLanguages.DETECT_LANG.value) {
                    setRecogniserIntent(
                        translateViewModel.availLanguagesValueToCodeMap[selectedItem]
                            ?: SupportedLanguages.ENGLISH.code
                    )
                    // comment this to stop translation when changing spinner
                    if (binding.ToLang.selectedItem != SupportedLanguages.DETECT_LANG.value
                        && !binding.sourceText.text.isNullOrBlank()
                    ) {
                        translateText()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun toLanguageSpinnerItemChangeListener() {
        binding.ToLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                Log.i(classTag(), "ToLang selected item: $selectedItem")
                if (selectedItem != SupportedLanguages.DETECT_LANG.value) {
                    setLanguageForResultTextToSpeech(selectedItem)
                    // comment this to stop translation when changing spinner
                    if (binding.FromLang.selectedItem != SupportedLanguages.DETECT_LANG.value
                        && !binding.sourceText.text.isNullOrBlank()
                    ) {
                        translateText()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun translationErrorObserver() {
        translateViewModel.translateErrorState.observe(this) {
            Log.i(classTag(), "Some error occurred while translating")
            // not showing this to user for now
        }
    }

    private fun translationModelDownloadErrorObserver() {
        translateViewModel.downloadErrorState.observe(this) {
            if (it) {
                shortToast(this, getString(R.string.model_download_error))
                translateViewModel.initializeTranslationState()
            }
        }
    }

    private fun translatedTextObserver() {
        translateViewModel.translatedText.observe(this) {
            binding.resultText.setText(it.toString())
        }
    }

    // can optimize it
    private fun showAlertBox(
        langCodes: List<String>,
        callback: (Boolean) -> (Unit)
    ) {
        val msg: String = if (langCodes.size == 2) {
            val firstLang = translateViewModel.supportedLanguagesCodeToValueMap[langCodes[0]]
            val secondLang = translateViewModel.supportedLanguagesCodeToValueMap[langCodes[1]]
            getString(R.string.download_model_confirmation_2, firstLang, secondLang)
        } else {
            val lang = translateViewModel.supportedLanguagesCodeToValueMap[langCodes[0]]
            getString(R.string.download_model_confirmation_1, lang)
        }

        val builder = AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(getString(R.string.yes), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .setCancelable(false)
            .show()
        doNotShowAlertBoxFlag = true

        builder.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            callback(true)
            builder.dismiss()
        }

        builder.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            callback(false)
            builder.dismiss()
        }

    }

    private fun modelDownloadProgressDialogInit() {
        val view: View =
            layoutInflater.inflate(R.layout.translation_download_progress_dialog_layout, null)

        modelDownloadDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
    }

    private fun translateModelDownloadObserver() {
        translateViewModel.newModelDownloadProgress.observe(this) {
            if (it) {
                modelDownloadDialog.show()
            } else {
                modelDownloadDialog.dismiss()
            }
        }
    }

    private fun ocrResultObserver() {
        translateViewModel.ocrResult.observe(this) {
            binding.sourceText.setText(it)
        }
    }

    private fun translateText() {
        if (binding.FromLang.selectedItem == SupportedLanguages.DETECT_LANG.value
            || binding.ToLang.selectedItem == SupportedLanguages.DETECT_LANG.value
        ) {
            shortToast(this, getString(R.string.select_language))
            return
        }
        val sourceLangCode =
            translateViewModel.availLanguagesValueToCodeMap[binding.FromLang.selectedItem.toString()]
        val targetLangCode =
            translateViewModel.availLanguagesValueToCodeMap[binding.ToLang.selectedItem.toString()]

        Log.i(classTag(), "codes are $sourceLangCode $targetLangCode")
        if (sourceLangCode == null || targetLangCode == null) {
            shortToast(this, getString(R.string.translation_error))
            translateViewModel.initializeTranslationState()
            return
        }
        translateViewModel.checkIfModelIsPresent(
            sourceLangCode, targetLangCode
        ) {
            if (it.isEmpty()) {
                translateViewModel.myTranslate(
                    binding.sourceText.text.toString(),
                    sourceLangCode,
                    targetLangCode,
                )
                return@checkIfModelIsPresent
            }
            if (doNotShowAlertBoxFlag) {
                return@checkIfModelIsPresent
            }
            showAlertBox(it) { userResponse ->
                if (userResponse) {
                    if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                        shortToast(this, getString(R.string.internet_error))
                        return@showAlertBox
                    }
                    translateViewModel.downloadNewModels(sourceLangCode, targetLangCode)
                    translateViewModel.myTranslate(
                        binding.sourceText.text.toString(),
                        sourceLangCode,
                        targetLangCode,
                    )
                }
            }
        }
    }

    private fun sourceTextChangeListener() {
        binding.sourceText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (makeTranslationDisabled) {
                    if (p0.isNullOrEmpty()) {
                        makeTranslationDisabled = false
                    }
                    return
                }

                if (p0.isNullOrEmpty()) {
                    // comment this so spinner will not reset on clear text
//                    translateViewModel.langDetectionState = true
//                    setLanguageInFromLanguageSpinner(AvailLanguages.DETECT_LANG.value)
                    binding.resultText.text.clear()
                    return
                }
                Log.i(classTag(), "Coming here")

                if (!p0.isNullOrBlank() && p0.length > 4 && translateViewModel.langDetectionState) {
                    Log.i(classTag(), "Here inside")
                    translateViewModel.identifyLanguage(p0.toString())
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    sourceTextIconsVisibilityListener(p0.toString())
                }, 500)
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0.isNullOrEmpty()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        doNotShowAlertBoxFlag = false
                    }, 1000)
                }

//                Handler(Looper.getMainLooper()).postDelayed({
//                    val spinnersAreSet =
//                        binding.FromLang.selectedItem != SupportedLanguages.DETECT_LANG.value
//                                && binding.ToLang.selectedItem != SupportedLanguages.DETECT_LANG.value
//                }, 1000)

                // comment this to stop automatic translation
                if (!p0.isNullOrEmpty() &&
                    binding.FromLang.selectedItem != SupportedLanguages.DETECT_LANG.value
                    && binding.ToLang.selectedItem != SupportedLanguages.DETECT_LANG.value
                ) {
                    translateText()
                }
            }
        })
    }

    private fun resultTextChangeListener() {
        binding.resultText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.resultText.isEnabled = !p0.isNullOrEmpty()
                resultTextIconsVisibilityListener(p0?.toString())
            }

            override fun afterTextChanged(p0: Editable?) {
                // :) ... very interesting bug
                if (binding.sourceText.text.length < 2) {
                    binding.resultText.text.clear()
                }
            }

        })
    }

    private fun sourceTextIconsVisibilityListener(text: String) {
        if (text.isEmpty()) {
            binding.sourceTextTalk.visibility = View.GONE
            binding.sourceTextCopy.visibility = View.GONE
            binding.sourceTextClear.visibility = View.GONE
        } else {
            binding.sourceTextTalk.visibility =
                if (makeSourceTextTalkDisabled) View.GONE else View.VISIBLE
            binding.sourceTextCopy.visibility = View.VISIBLE
            binding.sourceTextClear.visibility = View.VISIBLE
        }
    }

    private fun sourceTextTalkIconListener() {
        binding.sourceTextTalk.setOnClickListener {
            if (binding.FromLang.selectedItem == SupportedLanguages.DETECT_LANG.value) {
                shortToast(this, getString(R.string.select_language))
                return@setOnClickListener
            }
            if (textToSpeechSourceText.isSpeaking) {
                textToSpeechSourceText.stop()
            }
            textToSpeechSourceText.speak(
                binding.sourceText.text.toString(),
                TextToSpeech.QUEUE_FLUSH,
                null,
                ""
            )
        }
    }

    private fun sourceTextCopyIconListener() {
        binding.sourceTextCopy.setOnClickListener {
            copyToClipBoard(binding.sourceText.text.toString())
        }
    }

    private fun sourceTextClearIconListener() {
        binding.sourceTextClear.setOnClickListener {
            binding.sourceText.text.clear()
            binding.resultText.text.clear()
            sourceTextIconsVisibilityListener(binding.sourceText.text.toString())
        }
    }

    private fun copyToClipBoard(text: String) {
        val clipboardManager: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboardManager.setPrimaryClip(clip)
    }

    private fun textToSpeechInit() {
        textToSpeechSourceText = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e(classTag(), "text to speech initialization error")
            }
        }
        textToSpeechResultText = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e(classTag(), "text to speech initialization error")
            }
        }
    }

    private fun setLanguageForSourceTextToSpeech(language: String) {
        if (language == SupportedLanguages.DETECT_LANG.value) return
        Log.i(classTag(), "source language to speak $language")
        val locale =
            translateViewModel.availLanguagesValueToCodeMap[language]?.let {
                Locale(it)
            }
        if (locale == null) {
            Log.e(classTag(), "Unable to find locale")
            makeSourceTextTalkDisabled = true
            binding.sourceTextTalk.visibility = View.GONE
            return
        }
        val result = textToSpeechSourceText.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(classTag(), "The $language Language is not supported!")
            makeSourceTextTalkDisabled = true
            binding.sourceTextTalk.visibility = View.GONE
        } else {
            makeSourceTextTalkDisabled = false
            if (!binding.sourceText.text.isNullOrBlank()) {
                binding.sourceTextTalk.visibility = View.VISIBLE
            }
        }
    }

    private fun resultTextIconsVisibilityListener(text: String?) {
        if (text.isNullOrEmpty()) {
            binding.resultTextTalk.visibility = View.GONE
            binding.resultTextCopy.visibility = View.GONE
            binding.resultTextClear.visibility = View.GONE
        } else {
            binding.resultTextTalk.visibility =
                if (makeResultTextTalkDisabled) View.GONE else View.VISIBLE
            binding.resultTextCopy.visibility = View.VISIBLE
            binding.resultTextClear.visibility = View.VISIBLE
        }
    }

    private fun resultTextTalkIconListener() {
        binding.resultTextTalk.setOnClickListener {
            if (binding.ToLang.selectedItem == SupportedLanguages.DETECT_LANG.value) {
                shortToast(this, getString(R.string.select_language))
                return@setOnClickListener
            }
            if (textToSpeechResultText.isSpeaking) {
                textToSpeechResultText.stop()
            }
            textToSpeechResultText.speak(
                binding.resultText.text.toString(),
                TextToSpeech.QUEUE_FLUSH,
                null,
                ""
            )
        }
    }

    private fun resultTextCopyIconListener() {
        binding.resultTextCopy.setOnClickListener {
            copyToClipBoard(binding.resultText.text.toString())
        }
    }

    private fun resultTextClearIconListener() {
        binding.resultTextClear.setOnClickListener {
            binding.resultText.text.clear()
        }
    }

    private fun setLanguageForResultTextToSpeech(language: String) {
        Log.i(classTag(), "result language to speak $language")
        val locale =
            translateViewModel.availLanguagesValueToCodeMap[language]?.let {
                Locale(it)
            }
        if (locale == null) {
            Log.e(classTag(), "Unable to find locale")
            makeResultTextTalkDisabled = true
            binding.resultTextTalk.visibility = View.GONE
            return
        }
        val result = textToSpeechResultText.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            makeResultTextTalkDisabled = true
            Log.e(classTag(), "The $language Language is not supported!")
            binding.resultTextTalk.visibility = View.GONE
        } else {
            makeResultTextTalkDisabled = false
            if (!binding.resultText.text.isNullOrBlank()) {
                binding.resultTextTalk.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechSourceText?.shutdown()
        textToSpeechResultText?.shutdown()
        val cacheImagesDir = File(cacheDir, CACHE_IMAGE_FOLDER)
        Log.i(classTag(), "all cache file ${cacheDir.listFiles()}")
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
        deleteDirectory(cacheImagesDir)
    }

    private fun deleteDirectory(directory: File) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
        directory.delete()
    }

    private fun startListening() {
        speechRecognizer!!.startListening(recognizerIntent)
    }

    private fun requestVoiceRecordPermissionIfNot(context: Context): Boolean {
        val permissionCheck =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        return if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestMultiplePermissions(
                this,
                PERMISSIONS_REQUEST_RECORD_AUDIO,
                Manifest.permission.RECORD_AUDIO
            )
            false
        } else {
            true
        }
    }

    private fun requestMultiplePermissions(
        activity: Activity,
        requestCode: Int,
        vararg permissions: String
    ) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            requestCode
        )
    }

    private fun isPermissionPermanentlyDenied(
        activity: Activity,
        permission: String
    ): Boolean {
        return !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(classTag(), "Record permission granted")
                startListening()
                return
            }
            if (isPermissionPermanentlyDenied(this, Manifest.permission.RECORD_AUDIO)) {
                goToAppSettings()
                return
            }
            shortToast(this, getString(R.string.microphone_permission_denied))
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED

                if (read && write) {
                    getImageFromGallery.launch("image/*")
                    return
                }
                if (isPermissionPermanentlyDenied(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || isPermissionPermanentlyDenied(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    goToAppSettings()
                    return
                }
                shortToast(this, getString(R.string.storage_permissions_denied))
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(classTag(), "camera permission granted")
                launchCamera()
                return
            }
            if (isPermissionPermanentlyDenied(this, Manifest.permission.CAMERA)) {
                goToAppSettings()
                return
            }
            shortToast(this, getString(R.string.camera_permission_denied))
        }
    }

    private fun goToAppSettings() {
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer != null) speechRecognizer!!.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer!!.setRecognitionListener(mRecognitionListener())
        } else {
            Log.i(classTag(), "Error in initialization speechRecognizer")
        }
    }

    private fun mRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(p0: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray) {
            }

            override fun onEndOfSpeech() {
                speechRecognizer!!.stopListening()
                binding.sourceTextListenStop.visibility = View.GONE
                binding.sourceTextMic.visibility = View.VISIBLE
            }

            override fun onResults(results: Bundle) {
                val matches: ArrayList<String>? = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var text = ""
                for (result in matches!!) {
                    text += """
                        $result
                        """.trimIndent()
                }
                binding.sourceText.setText(text)
                if (IS_CONTINUES_LISTEN) {
                    startListening()
                }
            }

            override fun onError(errorCode: Int) {
                // error toast here try again later
                Log.i(classTag(), "Error while listening to speech")
                // reset SpeechRecognizer
                initSpeechRecognizer()
//            startListening()
            }

            override fun onEvent(arg0: Int, arg1: Bundle) {
            }

            override fun onPartialResults(arg0: Bundle) {
            }

            override fun onReadyForSpeech(arg0: Bundle) {
                binding.sourceTextListenStop.visibility = View.VISIBLE
                binding.sourceTextMic.visibility = View.GONE
                binding.sourceText.text.clear()
            }
        }
    }

    private fun setRecogniserIntent(selectedLanguage: String) {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            selectedLanguage
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            selectedLanguage
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, RESULTS_LIMIT)
    }

    private fun textContainerInit() {
        binding.sourceText.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.sourceText.setRawInputType(InputType.TYPE_CLASS_TEXT)
        binding.resultText.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.resultText.setRawInputType(InputType.TYPE_CLASS_TEXT)
    }


    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { imageUri ->
            Log.i(classTag(), "GOT image from gallery is $imageUri")
            if (imageUri == null) {
                Log.i(classTag(), "Uri is null")
                return@registerForActivityResult
            }
            val intent = Intent(this, CropActivity::class.java)
            intent.putExtra(IMAGE_URI, imageUri.toString())
            resultLauncher.launch(intent)
        }

    private val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val imageUri = data?.data ?: return@registerForActivityResult
            val language = binding.FromLang.selectedItem.toString()
            translateViewModel.runTextRecognition(applicationContext, language, imageUri)
        }

        if (result.resultCode == RESULT_CANCELED) {
            Log.i(classTag(), "Crop image was cancelled")
        }
    }

    private fun requestStoragePermissionIfNot(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            true
        } else if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestForStoragePermissionsBelowAndroid11()
            false
        }
    }

    private fun requestForStoragePermissionsBelowAndroid11() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            //Below android 11
            requestMultiplePermissions(
                this,
                STORAGE_PERMISSION_REQUEST_CODE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private var latestTmpUri: Uri? = null

    private val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { uri ->
                    val intent = Intent(this, CropActivity::class.java)
                    intent.putExtra(IMAGE_URI, uri.toString())
                    resultLauncher.launch(intent)
                }
            } else {
                Log.d(classTag(), "Camera failed")
            }
        }

    private fun requestCameraPermissionIfNot(context: Context): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestMultiplePermissions(this, REQUEST_CAMERA_PERMISSION, Manifest.permission.CAMERA)
            false
        } else {
            true
        }
    }

    private fun launchCamera() {
        val tempFile = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.provider",
            getTempFile(this, ".png")
        )
        tempFile.let { uri ->
            latestTmpUri = uri
            takeImageResult.launch(uri)
        }
    }
}
