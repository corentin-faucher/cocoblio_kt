@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.coq.cocoblio

import android.content.Context
import java.io.IOException

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

        fun loadStringFromAssets(fileName: String, ctx: Context, showError: Boolean = true) : String? {
            return try {
                ctx.assets.open("${currentLanguage.iso}/$fileName").use { inputStream ->
                    inputStream.bufferedReader().use { bufferedReader ->
                        bufferedReader.readText()
                    }
                }
            } catch (e : IOException) {
                if(showError)
                    printerror("Ne peut charger \"$fileName\" dans \"${currentLanguage.iso}\".")
                null
            }
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
        /*private val longStringArray = arrayOf(
            "french",
            "english",
            "japanese",
            "german",
            "chinese_simpl",
            "italian",
            "spanish",
            "arabic",
            "greek",
            "russian",
            "swedish",
            "chinese_trad",
            "portuguese",
            "korean"
        )*/
    }
}
