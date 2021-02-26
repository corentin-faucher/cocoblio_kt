@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio.divers

enum class Language(val iso: String) {
    French("fr"),
    English("en"),
    Japanese("ja"),
    German("de"),
    ChineseSimpl("zh-Hans"),
    Italian("it"),
    Spanish("es"),
    Arabic("ar"),
    Greek("el"),
    Russian("ru"),
    Swedish("sv"),
    ChineseTrad("zh-Hant"),
    Portuguese("pt"),
    Korean("ko");

    companion object {
        fun setCurrentLanguage(stringCode: String) {
            currentLanguage = isoToEnumMap[stringCode] ?: English
        }

        fun currentIs(language: Language) : Boolean
                = currentLanguage == language

        var currentLanguage: Language = English
            private set
        val currentLanguageID: Int
            get() = currentLanguage.ordinal

        private val isoToEnumMap = mapOf(
            "fr" to  French,
            "en" to  English,
            "ja" to  Japanese,
            "de" to  German,
            "it" to  Italian,
            "es" to  Spanish,
            "ar" to  Arabic,
            "el" to  Greek,
            "ru" to  Russian,
            "sv" to  Swedish,
            "zh-Hans" to ChineseSimpl,
            "zh-Hant" to ChineseTrad,
            "pt" to    Portuguese,
            "ko" to    Korean
        )
    }
}
