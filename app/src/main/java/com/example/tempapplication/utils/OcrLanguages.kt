package com.example.tempapplication.utils

enum class OcrLanguages(private vararg val languages: String) {
    LATIN(), // for now keeping latin as default, later will show toast for unsupported languages
    CHINESE("Chinese"),
    DEVANAGARI("Hindi", "Sanskrit", "Marathi", "Nepali"),
    JAPANESE("Japanese"),
    KOREAN("Korean");

    companion object {
        fun findScript(language: String): OcrLanguages {
            return values().find { language in it.languages } ?: LATIN
        }
    }
}