package dev.legendsayantan.extendroid

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Lightweight in-app language switching.
 *
 * Supported values for [Prefs.language]:
 *  * [SYSTEM]  – follow the device language (default)
 *  * [ENGLISH] – force English
 *  * [CHINESE] – force Simplified Chinese
 *
 * Activities override [Activity.attachBaseContext] with [wrap], while contexts that are not
 * activities (services, overlays, notification builders) are wrapped manually through [wrap].
 *
 * @author Extendroid contributors
 */
object LocaleHelper {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val CHINESE = "zh-CN"

    /** All selectable languages, in the order they are shown to the user. */
    val selectable = listOf(SYSTEM, CHINESE, ENGLISH)

    fun storedTag(ctx: Context): String = Prefs(ctx.applicationContext).language

    fun localeOf(tag: String): Locale? = when (tag) {
        CHINESE -> Locale.SIMPLIFIED_CHINESE
        ENGLISH -> Locale.ENGLISH
        else -> null
    }

    /** Human readable name of a selectable language, already localized. */
    fun displayName(ctx: Context, tag: String): String = when (tag) {
        CHINESE -> ctx.getString(R.string.language_chinese)
        ENGLISH -> ctx.getString(R.string.language_english)
        else -> ctx.getString(R.string.language_system)
    }

    /**
     * Returns a context whose resources use the stored language.
     * When the stored language is [SYSTEM] the very same context is returned.
     */
    fun wrap(ctx: Context): Context {
        val locale = localeOf(storedTag(ctx)) ?: return ctx
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return ctx.createConfigurationContext(config)
    }

    /** Applies the stored language to the JVM wide default locale. */
    fun applyTo(ctx: Context) {
        val stored = localeOf(storedTag(ctx))
        val locale = stored ?: ctx.resources.configuration.locales[0]
        Locale.setDefault(locale ?: Locale.getDefault())
    }

    /** Persists [tag] and applies it to the whole process. */
    fun setLanguage(ctx: Context, tag: String) {
        Prefs(ctx.applicationContext).language = tag
        applyTo(ctx.applicationContext)
    }
}
