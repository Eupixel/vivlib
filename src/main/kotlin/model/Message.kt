package net.eupixel.model

class Message(private val key: String, private var translations: ArrayList<Translation>) {
    fun getKey(): String {
        return key
    }

    fun getTranslations(): ArrayList<Translation> {
        return translations
    }
}