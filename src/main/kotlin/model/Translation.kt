package net.eupixel.model

import java.util.Locale

class Translation(private val locale: Locale, private val message: String) {
    fun getLocale(): Locale {
        return locale
    }

    fun getMessage(): String {
        return message
    }
}