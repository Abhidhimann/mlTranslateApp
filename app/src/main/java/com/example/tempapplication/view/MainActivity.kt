package com.example.tempapplication.view

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tempapplication.R
import com.example.tempapplication.TranslateViewModel
import com.example.tempapplication.databinding.ActivityMainBinding
import com.example.tempapplication.utils.*
import kotlinx.coroutines.launch
import java.util.*


// later work change change hardcoded strings
// changing translation button ui
// adding seeing models and deleting them
// optional: text detection by image & voice, maybe dark theme
// refactor spinner adaptor
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fromLangSpinnerAdapter: ArrayAdapter<String>
    private lateinit var toLangSpinnerAdapter: ArrayAdapter<String>
    private lateinit var textToSpeechSourceText: TextToSpeech
    private lateinit var textToSpeechResultText: TextToSpeech
    private lateinit var fromLangCustomDialog: Dialog
    private lateinit var toLangCustomDialog: Dialog

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private lateinit var translateViewModel: TranslateViewModel
    private var makeTranslationDisabled = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        translateViewModel =
            ViewModelProvider(this)[TranslateViewModel::class.java]

        initialization()
        spinnerListeners()
        textChangeListeners()
        translationListeners()
        translationObservers()
        textOperationsIconsListeners()
        binding.toolbar.threeDotMenu.setOnClickListener {
            val popupMenu = PopupMenu(this@MainActivity, it)

            popupMenu.menuInflater.inflate(R.menu.menu_items, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                // Handle menu item clicks
                when (item.itemId) {
                    R.id.models -> {
                        // Handle menu item 1 click
                        Toast.makeText(
                            this@MainActivity,
                            "Menu Item 1 clicked",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun initialization() {
        languageSpinnerInit()
        textOperationsIconsVisibilityInit()
        textToSpeechInit()
        fromLangDialogSearchAbleSpinnerInit()
        toLangDialogSearchAbleSpinnerInit()
        initSpeechRecognizer()
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

    private fun translationListeners() {
        translateButtonClickListener()
    }

    private fun translationObservers() {
        languageIdentifierObserver()
        translationErrorObserver()
        translationModelDownloadErrorObserver()
        translatedTextObserver()
        translateModelDownloadObserver()
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

    private fun sourceTextMicListener() {
        binding.sourceTextMic.setOnClickListener {
            if (binding.FromLang.selectedItem == AvailLanguages.DETECT_LANG.value) {
                Toast.makeText(this, "Please select language", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkVoiceRecordPermissions()
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
        fromLangCustomDialog = Dialog(this)
        fromLangCustomDialog.setContentView(R.layout.searchable_spinner)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        fromLangCustomDialog.window?.setLayout(
            (screenWidth * 0.6).toInt(),
            (screenHeight * 0.7).toInt()
        )
        fromLangCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editText: EditText = fromLangCustomDialog.findViewById(R.id.edit_text)
        val listView: ListView = fromLangCustomDialog.findViewById(R.id.list_view)

        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_list_item_1,
            translateViewModel.availLanguagesCodeToValueMap.values.toList()
        ) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                Log.i(
                    tempTag(),
                    "avail and contains ${translateViewModel.availableModels} ${getItem(position)}"
                )
                val langCode = translateViewModel.availLanguagesValueToCodeMap[getItem(position)]
                if (translateViewModel.availableModels.contains(langCode)) {
                    textView.alpha = 1f
                } else {
                    textView.alpha = 0.5f
                }
                return view
            }
        }

        listView.adapter = adapter
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            binding.FromLang.setSelection(
                fromLangSpinnerAdapter.getPosition(
                    adapter.getItem(
                        position
                    )
                )
            )
            fromLangCustomDialog.dismiss()
        }
    }

    private fun toLangDialogSearchAbleSpinnerInit() {
        toLangCustomDialog = Dialog(this)
        toLangCustomDialog.setContentView(R.layout.searchable_spinner)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        toLangCustomDialog.window?.setLayout(
            (screenWidth * 0.5).toInt(),
            (screenHeight * 0.64).toInt()
        )
        toLangCustomDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editText: EditText = toLangCustomDialog.findViewById(R.id.edit_text)
        val listView: ListView = toLangCustomDialog.findViewById(R.id.list_view)

        val adapter: ArrayAdapter<String> = object : ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_list_item_1,
            translateViewModel.availLanguagesCodeToValueMap.values.filter { it != AvailLanguages.DETECT_LANG.value }
        ) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                Log.i(
                    tempTag(),
                    "avail and contains ${translateViewModel.availableModels} ${getItem(position)}"
                )
                val langCode = translateViewModel.availLanguagesValueToCodeMap[getItem(position)]
                if (translateViewModel.availableModels.contains(langCode)) {
                    textView.alpha = 1f
                } else {
                    textView.alpha = 0.5f
                }
                return view
            }
        }

        listView.adapter = adapter
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            binding.ToLang.setSelection(
                toLangSpinnerAdapter.getPosition(
                    adapter.getItem(
                        position
                    )
                )
            )
            toLangCustomDialog.dismiss()
        }
    }


    private fun languageSpinnerInit() {
        fromLangSpinnerAdapter = ArrayAdapter(
            this,
            R.layout.custom_spinner_item,
            translateViewModel.availLanguagesCodeToValueMap.values.toList()
        )
        binding.FromLang.adapter = fromLangSpinnerAdapter
        setLanguageInFromLanguageSpinner(AvailLanguages.DETECT_LANG.value)

        toLangSpinnerAdapter = ArrayAdapter(
            this,
            R.layout.custom_spinner_item,
            translateViewModel.availLanguagesCodeToValueMap.values.filter { it != AvailLanguages.DETECT_LANG.value }
                .toList()
        )
        binding.ToLang.adapter = toLangSpinnerAdapter
        setLanguageInToLanguageSpinner(AvailLanguages.ENGLISH.value)
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
            if (temp == AvailLanguages.DETECT_LANG.value) {
                binding.ToLang.setSelection(toLangSpinnerAdapter.getPosition(AvailLanguages.ENGLISH.value))
            } else {
                binding.ToLang.setSelection(toLangSpinnerAdapter.getPosition(temp))
            }
        }
    }

    private fun languageIdentifierObserver() {
        translateViewModel.identifiedLang.observe(this) { langCode ->
            if (binding.FromLang.selectedItem != AvailLanguages.DETECT_LANG.value) {
                return@observe
            }
            if (langCode == null || langCode == AvailLanguages.DETECT_LANG.code) {
                setLanguageInFromLanguageSpinner(AvailLanguages.DETECT_LANG.value)
                return@observe
            }
            val language = translateViewModel.availLanguagesCodeToValueMap[langCode.lowercase()]
            Log.i(tempTag(), "identified lang $language")
            if (language != null) {
                setLanguageInFromLanguageSpinner(language)
            } else {
                Toast.makeText(
                    this,
                    "Sorry ${Locale(langCode).displayLanguage} is not supported yet",
                    Toast.LENGTH_LONG
                ).show()
                makeTranslationDisabled = true
                binding.translate.isEnabled = false
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
                    selectedItem == AvailLanguages.DETECT_LANG.value
                if (translateViewModel.langDetectionState && binding.sourceText.text.length > 4) {
                    translateViewModel.identifyLanguage(binding.sourceText.text.toString())
                } else {
                    setLanguageForSourceTextToSpeech(selectedItem)
                }

                if (selectedItem != AvailLanguages.DETECT_LANG.value) {
                    setRecogniserIntent(
                        translateViewModel.availLanguagesValueToCodeMap[selectedItem]
                            ?: AvailLanguages.ENGLISH.code
                    )
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
                Log.i(tempTag(), "ToLang selected item: $selectedItem")
                if (selectedItem != AvailLanguages.DETECT_LANG.value) {
                    setLanguageForResultTextToSpeech(selectedItem)
                    // comment this to stop translation when changing spinner
                    if (binding.FromLang.selectedItem != AvailLanguages.DETECT_LANG.value
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
            Log.i(tempTag(), "Some error occurred while translating")
            // not showing this to user
        }
    }

    private fun translationModelDownloadErrorObserver() {
        translateViewModel.downloadErrorState.observe(this) {
            if (it) {
                Toast.makeText(
                    this,
                    "Some error occurred while downloading. Please try again later.",
                    Toast.LENGTH_SHORT
                ).show()
                translateViewModel.initializeTranslationState()
            }
        }
    }

    private fun translatedTextObserver() {
        translateViewModel.translatedText.observe(this) {
            binding.resultText.setText(it.toString())
        }
    }

    private fun showAlertBox(
        langCodes: List<String>,
        callback: (Boolean) -> (Unit)
    ) {
        val msg: String = if (langCodes.size == 2) {
            val firstLang = translateViewModel.availLanguagesCodeToValueMap[langCodes[0]]
            val secondLang = translateViewModel.availLanguagesCodeToValueMap[langCodes[1]]
            "Translation require downloading model for $firstLang & $secondLang" +
                    "(< 30mb). This is only one time process."
        } else {
            val lang = translateViewModel.availLanguagesCodeToValueMap[langCodes[0]]
            "Translation require downloading model for $lang (< 15mb). This is only one time process."
        }

        val builder = AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Yes", null)
            .setNegativeButton("Cancel", null)
            .show()

        builder.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            callback(true)
            builder.dismiss()
        }

        builder.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            callback(false)
            builder.dismiss()
        }

    }

    private fun translateModelDownloadObserver() {
        val view: View =
            layoutInflater.inflate(R.layout.translation_download_progress_dialog_layout, null)

        val modelDownloadDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        translateViewModel.newModelDownloadProgress.observe(this) {
            if (it) {
                modelDownloadDialog.show()
            } else {
                modelDownloadDialog.dismiss()
            }
        }
    }

    private fun translateButtonClickListener() {
        binding.translate.setOnClickListener {
            translateText()
        }
    }

    private fun translateText() {
        if (binding.FromLang.selectedItem == AvailLanguages.DETECT_LANG.value
            || binding.ToLang.selectedItem == AvailLanguages.DETECT_LANG.value
        ) {
            Toast.makeText(
                this,
                "Please Select Language",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val sourceLangCode =
            translateViewModel.availLanguagesValueToCodeMap[binding.FromLang.selectedItem.toString()]
        val targetLangCode =
            translateViewModel.availLanguagesValueToCodeMap[binding.ToLang.selectedItem.toString()]

        Log.i(tempTag(), "codes are $sourceLangCode $targetLangCode")
        if (sourceLangCode == null || targetLangCode == null) {
            Toast.makeText(
                this,
                "Some error occurred while translating. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
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
            showAlertBox(it) { userResponse ->
                if (userResponse) {
                    lifecycleScope.launch {
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

                binding.translate.isEnabled = !p0.isNullOrBlank()

                if (p0.isNullOrEmpty()) {
                    // comment this so spinner will not reset on clear text
//                    translateViewModel.langDetectionState = true
//                    setLanguageInFromLanguageSpinner(AvailLanguages.DETECT_LANG.value)
                    binding.resultText.text.clear()
                }
                Log.i(tempTag(), "Coming here")
                sourceTextIconsVisibilityListener(p0?.toString())

                if (!p0.isNullOrBlank() && p0.length > 4 && translateViewModel.langDetectionState) {
                    Log.i(tempTag(), "Here inside")
                    translateViewModel.identifyLanguage(p0.toString())
                }

                // comment this to stop automatic translation
                if (!p0.isNullOrEmpty()
                    && binding.FromLang.selectedItem != AvailLanguages.DETECT_LANG.value
                    && binding.ToLang.selectedItem != AvailLanguages.DETECT_LANG.value
                ) {
                    translateText()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
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

    private fun sourceTextIconsVisibilityListener(text: String?) {
        if (text.isNullOrEmpty()) {
            binding.sourceTextTalk.visibility = View.GONE
            binding.sourceTextCopy.visibility = View.GONE
            binding.sourceTextClear.visibility = View.GONE
        } else {
            binding.sourceTextTalk.visibility = View.VISIBLE
            binding.sourceTextCopy.visibility = View.VISIBLE
            binding.sourceTextClear.visibility = View.VISIBLE
        }
    }

    private fun sourceTextTalkIconListener() {
        binding.sourceTextTalk.setOnClickListener {
            if (binding.FromLang.selectedItem == AvailLanguages.DETECT_LANG.value) {
                Toast.makeText(
                    this,
                    "Please Selected Language.",
                    Toast.LENGTH_SHORT
                ).show()
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
                Log.e(tempTag(), "text to speech initialization error")
            }
        }
        textToSpeechResultText = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e(tempTag(), "text to speech initialization error")
            }
        }
    }

    private fun setLanguageForSourceTextToSpeech(language: String) {
        Log.i(tempTag(), "source language to speak $language")
        val locale =
            translateViewModel.availLanguagesValueToCodeMap[language]?.let {
                Locale(it)
            }
        if (locale == null) {
            Log.e(tempTag(), "Unable to find locale")
            binding.sourceTextTalk.visibility = View.GONE
            return
        }
        val result = textToSpeechSourceText.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(tempTag(), "The $language Language is not supported!")
            binding.sourceTextTalk.visibility = View.GONE
        } else {
            if (!binding.resultText.text.isNullOrBlank()) {
                binding.resultTextTalk.visibility = View.VISIBLE
            }
        }
    }

    private fun resultTextIconsVisibilityListener(text: String?) {
        if (text.isNullOrEmpty()) {
            binding.resultTextTalk.visibility = View.GONE
            binding.resultTextCopy.visibility = View.GONE
            binding.resultTextClear.visibility = View.GONE
        } else {
            binding.resultTextTalk.visibility = View.VISIBLE
            binding.resultTextCopy.visibility = View.VISIBLE
            binding.resultTextClear.visibility = View.VISIBLE
        }
    }

    private fun resultTextTalkIconListener() {
        binding.resultTextTalk.setOnClickListener {
            if (binding.ToLang.selectedItem == AvailLanguages.DETECT_LANG.value) {
                Toast.makeText(
                    this,
                    "Please Selected Language",
                    Toast.LENGTH_SHORT
                ).show()
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
        Log.i(tempTag(), "result language to speak $language")
        val locale =
            translateViewModel.availLanguagesValueToCodeMap[language]?.let {
                Locale(it)
            }
        if (locale == null) {
            Log.e(tempTag(), "Unable to find locale")
            binding.resultTextTalk.visibility = View.GONE
            return
        }
        val result = textToSpeechResultText.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(tempTag(), "The $language Language is not supported!")
            binding.resultTextTalk.visibility = View.GONE
        } else {
            if (!binding.resultText.text.isNullOrBlank()) {
                binding.resultTextTalk.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeechSourceText?.shutdown()
        textToSpeechResultText?.shutdown()
    }

    private fun startListening() {
        speechRecognizer!!.startListening(recognizerIntent)
    }

    private fun checkVoiceRecordPermissions() {
        val permissionCheck =
            ContextCompat.checkSelfPermission(applicationContext, "android.permission.RECORD_AUDIO")
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf("android.permission.RECORD_AUDIO"),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(tempTag(), "Record permission granted")
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer != null) speechRecognizer!!.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer!!.setRecognitionListener(mRecognitionListener())
        } else {
            Log.i(tempTag(), "Error in initialization speechRecognizer")
        }
    }

    fun mRecognitionListener(): RecognitionListener {
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
                Log.i(tempTag(), "Error while listening to speech")

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
}
