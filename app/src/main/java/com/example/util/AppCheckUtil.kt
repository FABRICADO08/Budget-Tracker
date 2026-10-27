package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

/**
 * Installs the provider before the app starts using Firebase.
 *
 * Release builds fail closed when Firebase or the App Check provider is misconfigured. App Check
 * must also be enforced for the relevant Firebase products in the Firebase console; client-side
 * setup is not a replacement for server-side enforcement.
 */
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            checkNotNull(FirebaseApp.initializeApp(context)) {
                "Firebase default app configuration is missing."
            }
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                check(prefs.edit().putString(DEBUG_SECRET_KEY, debugToken).commit()) {
                    "Could not persist the debug App Check token."
                }
                Log.d(TAG, "App Check debug token configured.")
            } else {
                Log.w(TAG, "No debug token supplied; using the Firebase debug provider.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            check(resId != 0) { "The release App Check site key resource is missing." }
            val siteKey = context.getString(resId)
            check(siteKey.isNotBlank()) { "The release App Check site key is empty." }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                RecaptchaAppCheckProviderFactory.getInstance(siteKey)
            )
        }
    } catch (error: Exception) {
        // Never log provider exceptions, tokens, or configuration details.
        Log.e(TAG, "App Check provider initialization failed.")
        if (!BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Secure startup is unavailable because App Check could not be initialized.",
                error
            )
        }
    }
}
