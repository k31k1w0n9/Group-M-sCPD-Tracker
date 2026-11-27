package com.example.scpd_tracker.data.firebase

import android.content.Context
import com.example.scpd_tracker.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/** The Firebase handles the repositories share. */
class FirebaseHandles(val auth: FirebaseAuth, val db: FirebaseFirestore)

/**
 * Starts Firebase without google-services.json, from the values in BuildConfig (see app/build.gradle.kts).
 * By default it talks to the free Local Emulator Suite on the laptop, with the demo project "demo-scpd".
 */
object FirebaseSetup {
    @Volatile private var handles: FirebaseHandles? = null

    fun get(context: Context): FirebaseHandles = handles ?: synchronized(this) {
        handles ?: create(context.applicationContext).also { handles = it }
    }

    private fun create(context: Context): FirebaseHandles {
        val options = FirebaseOptions.Builder()
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .build()
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context, options)
        val auth = FirebaseAuth.getInstance(app)
        val db = FirebaseFirestore.getInstance(app)
        // Release builds always use the real project: only debug builds may talk to the local emulators
        // (and only debug builds allow the plain HTTP they need, see src/debug/res/xml/network_security_config.xml).
        val host = BuildConfig.FIREBASE_EMULATOR_HOST
        if (BuildConfig.DEBUG && host.isNotBlank()) {
            auth.useEmulator(host, 9099)
            // The emulator has no Play Integrity or reCAPTCHA, so skip app verification there (never on the live project).
            auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            db.useEmulator(host, 8080)
        }
        // Keep a copy of what the student has seen on disk, so Explore and My Events still work offline.
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        return FirebaseHandles(auth, db)
    }
}
