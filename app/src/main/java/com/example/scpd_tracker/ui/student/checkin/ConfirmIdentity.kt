package com.example.scpd_tracker.ui.student.checkin

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface IdentityResult {
    data object Confirmed : IdentityResult
    data object Cancelled : IdentityResult
    /** The phone has no fingerprint, face or screen lock, so there is nothing to confirm with. */
    data object NoScreenLock : IdentityResult
}

private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * Asks the phone's owner to confirm it's them before a check-in is sent. It makes "hand your unlocked phone to
 * a friend" harder; it can't stop someone who knows the screen lock, which the report lists as a weakness.
 */
fun confirmIdentity(activity: FragmentActivity, onResult: (IdentityResult) -> Unit) {
    if (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
        onResult(IdentityResult.NoScreenLock)
        return
    }
    val prompt = BiometricPrompt(
        activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(IdentityResult.Confirmed)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(IdentityResult.Cancelled)
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm it's you")
            .setSubtitle("Check-ins only count for the account owner.")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build(),
    )
}
