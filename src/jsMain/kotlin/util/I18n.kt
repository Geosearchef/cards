package util

object I18n {
    fun get(key: String) = js("i18nGet(key)") as? String ?: ""
}