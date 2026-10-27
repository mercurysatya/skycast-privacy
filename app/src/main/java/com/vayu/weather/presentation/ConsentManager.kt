package com.vayu.weather.presentation

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vayu.weather.BuildConfig

object ConsentManager {

    private const val TAG = "ConsentManager"

    /**
     * Debug/test ONLY — forces UMP to evaluate the device as if it were inside
     * the EEA so the owner-published European Ads Privacy & Messaging message
     * can be verified from a non-EEA lab location. The hash was confirmed live:
     * `UserMessagingPlatform: Use new ConsentDebugSettings.Builder()
     * .addTestDeviceHashedId("D6E0A4A0826D05C3C4E71F9D29FC3EB9")...`
     * (logcat `artifacts/logcat_ump_fresh2.txt:3811`, 2026-10-18).
     * Gated by `BuildConfig.DEBUG`, so the release variant never sets debug
     * geography and behaves purely by real location.
     */
    private const val DEBUG_TEST_DEVICE_HASH = "D6E0A4A0826D05C3C4E71F9D29FC3EB9"

    @Volatile
    private var consentInformation: ConsentInformation? = null

    private fun getConsentInfo(context: Context): ConsentInformation {
        return consentInformation ?: UserMessagingPlatform.getConsentInformation(
            context.applicationContext
        ).also { consentInformation = it }
    }

    @JvmStatic
    fun initialize(context: Context) {
        getConsentInfo(context)
    }

    @JvmStatic
    fun canRequestAds(context: Context): Boolean {
        return try {
            getConsentInfo(context).canRequestAds()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check consent", e)
            false
        }
    }

    /**
     * Requests consent info update and shows the consent form if required.
     * Safe to call on every app start - required by Google's EU User Consent Policy.
     */
    @JvmStatic
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        try {
            val consentInfo = getConsentInfo(activity)
            val params =
                if (BuildConfig.DEBUG) {
                    // Debug/test build only. Never set debug geography in a
                    // release build.
                    ConsentRequestParameters.Builder()
                        .setConsentDebugSettings(
                            ConsentDebugSettings.Builder(activity)
                                .setDebugGeography(
                                    ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
                                )
                                .addTestDeviceHashedId(DEBUG_TEST_DEVICE_HASH)
                                .build()
                        )
                        .build()
                } else {
                    ConsentRequestParameters.Builder().build()
                }
            consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        formError?.let {
                            Log.w(TAG, "Consent form error: ${it.message}")
                        }
                        onComplete()
                    }
                },
                { requestError ->
                    Log.w(TAG, "Consent info update failed: ${requestError.message}")
                    onComplete()
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Consent gathering failed", e)
            onComplete()
        }
    }

    /** Whether a privacy options entry point is required (EEA/UK users). */
    @JvmStatic
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            getConsentInfo(context).privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (e: Exception) {
            false
        }
    }

    /** Shows the privacy options form so the user can change their consent choice. */
    @JvmStatic
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                formError?.let {
                    Log.w(TAG, "Privacy options form error: ${it.message}")
                }
                onDismissed()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Privacy options form failed", e)
            onDismissed()
        }
    }

    @JvmStatic
    fun resetConsentInfo(context: Context) {
        try {
            getConsentInfo(context).reset()
        } catch (e: Exception) {
            Log.w(TAG, "Consent reset failed", e)
        }
    }
}
