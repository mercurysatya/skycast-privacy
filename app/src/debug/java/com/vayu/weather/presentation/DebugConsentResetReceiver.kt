package com.vayu.weather.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vayu.weather.BuildConfig

/**
 * Debug/test helper ONLY — this class lives in the `debug` source set, so it is
 * neither compiled nor merged into release variants.
 *
 * Clears stored UMP consent state so the next `gatherConsent()` treats the
 * device as consent-free and the full form path can be verified cleanly.
 *
 * Usage (wireless ADB, debug build only):
 * `adb -s SERIAL shell am broadcast -a com.vayu.weather.DEBUG_RESET_CONSENT
 *          -n com.vayu.weather/.presentation.DebugConsentResetReceiver`
 */
class DebugConsentResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return // safety net — should be impossible here
        if (intent.action != ACTION) return

        ConsentManager.resetConsentInfo(context)
        Log.i(TAG, "UMP consent state reset (debug build helper)")
    }

    companion object {
        const val TAG = "DebugConsentReset"
        const val ACTION = "com.vayu.weather.DEBUG_RESET_CONSENT"
    }
}
