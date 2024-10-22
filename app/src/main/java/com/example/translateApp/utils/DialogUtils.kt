package com.example.translateApp.utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import com.example.tempapplication.R

object DialogUtils {
    fun dialogSearchAbleSpinnerInit(
        activity: Activity, displayMetrics: DisplayMetrics,
        supportedLanguages: List<SupportedLanguages>,
        availableModels: List<String>,
        onItemClick: (SupportedLanguages) -> Unit,
    ): Dialog {
        val customDialog = Dialog(activity)
        customDialog.setContentView(R.layout.searchable_spinner)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        customDialog.window?.setLayout(
            (screenWidth * 0.54).toInt(),
            (screenHeight * 0.7).toInt()
        )
        customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val editText: EditText = customDialog.findViewById(R.id.edit_text)
        val listView: ListView = customDialog.findViewById(R.id.list_view)

        val adapter: ArrayAdapter<SupportedLanguages> = object : ArrayAdapter<SupportedLanguages>(
            activity,
            android.R.layout.simple_list_item_1,
            supportedLanguages
        ) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                val langCode = getItem(position)?.code
                val language = getItem(position)?.value ?: SupportedLanguages.ENGLISH.value
                textView.text = language
                textView.setTextColor(Color.BLACK)
                if (availableModels.contains(langCode)) {
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
            onItemClick(adapter.getItem(position) ?: SupportedLanguages.ENGLISH)
        }
        return customDialog
    }
}