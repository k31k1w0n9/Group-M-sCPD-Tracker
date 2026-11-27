package com.example.scpd_tracker.data.firebase

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.DeviceIdSource
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant

/** How long a network call may take before the app treats the phone as offline. */
internal const val FIREBASE_TIMEOUT_MS = 15_000L
private const val TAG = "ScpdAuth"

/**
 * Sign-in with Firebase Auth. Firebase keeps the session on disk, and the role the student picked is saved in
 * DataStore, so the app is still signed in after Android kills it (for example when a permission is switched off
 * in Settings while the app is in the background).
 */
class FirebaseAuthRepository(
    private val fb: FirebaseHandles,
    private val network: NetworkMonitor,
    private val store: DataStore<Preferences>,
    private val deviceId: DeviceIdSource,
    scope: CoroutineScope,
) : AuthRepository {
    private val roleKey = stringPreferencesKey("signed_in_role")
    private val user = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = user.asStateFlow()

    init {
        scope.launch { restore() }
    }

    private suspend fun restore() {
        val signedIn = fb.auth.currentUser ?: return
        val role = store.data.first()[roleKey]?.let { saved -> AccountRole.entries.firstOrNull { it.name == saved } } ?: return
        // Works offline too: Firestore answers from its disk cache.
        runCatching { loadProfile(signedIn.uid, signedIn.email.orEmpty(), role) }.getOrNull()
            ?.takeIf { it.canUse(role) }
            ?.let { user.value = it }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun loadProfile(uid: String, email: String, role: AccountRole): UserProfile? {
        val doc = fb.db.collection(Collections.USERS).document(uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(
            uid = uid,
            displayName = doc.getString("displayName").orEmpty(),
            email = doc.getString("email") ?: email,
            role = role,
            studentId = doc.getString("studentId"),
            // Shown in the app only. The rules check clubs/{clubId}/members/{uid} for every committee write.
            clubIds = (doc.get("clubIds") as? List<String>).orEmpty(),
            interests = (doc.get("interests") as? List<String>).orEmpty(),
            hasChosenInterests = doc.getBoolean("hasChosenInterests") ?: false,
        )
    }

    override suspend fun signIn(email: String, password: String, requestedRole: AccountRole): SignInResult {
        if (!network.isOnline.value) return SignInResult.Offline.also { Log.w(TAG, "sign-in: phone reports no connection") }
        val uid = try {
            withTimeout(FIREBASE_TIMEOUT_MS) {
                fb.auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            } ?: return SignInResult.InvalidCredentials
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            return SignInResult.InvalidCredentials
        } catch (_: FirebaseAuthInvalidUserException) {
            return SignInResult.InvalidCredentials
        } catch (e: FirebaseNetworkException) {
            Log.w(TAG, "sign-in: network error", e)
            return SignInResult.Offline
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "sign-in: timed out", e)
            return SignInResult.Offline
        }
        val profile = runCatching { loadProfile(uid, email.trim(), requestedRole) }.getOrNull()
        if (profile == null) {
            fb.auth.signOut()
            return SignInResult.InvalidCredentials
        }
        if (!profile.canUse(requestedRole)) {
            fb.auth.signOut()
            return SignInResult.RoleNotAllowed
        }
        store.edit { it[roleKey] = requestedRole.name }
        if (requestedRole == AccountRole.Student) bindDevice(uid)
        user.value = profile
        return SignInResult.Success(profile)
    }

    /**
     * One account, one phone. The first phone is bound at once; moving to another phone is allowed after 24 hours.
     * If the rules refuse (cooldown), sign-in still works, but check-ins from this phone are saved as Under review.
     */
    private suspend fun bindDevice(uid: String) {
        val mine = deviceId.get()
        val ref = fb.db.collection(Collections.USERS).document(uid)
        runCatching {
            val doc = ref.get().await()
            val bound = doc.getString("deviceId")
            val boundAt = doc.getTimestamp("deviceBoundAt")?.toInstantValue()
            val cooled = boundAt == null || Instant.now().isAfter(boundAt.plus(Duration.ofHours(24)))
            if (bound != mine && (bound == null || cooled)) {
                withTimeout(FIREBASE_TIMEOUT_MS) {
                    ref.update(mapOf("deviceId" to mine, "deviceBoundAt" to FieldValue.serverTimestamp())).await()
                }
            }
        }
    }

    override suspend fun signOut() {
        store.edit { it.remove(roleKey) }
        fb.auth.signOut()
        user.value = null
    }

    override suspend fun saveInterests(interests: List<String>) {
        val current = user.value ?: return
        // Saved to the disk cache at once and sent when online, so this works offline as well.
        fb.db.collection(Collections.USERS).document(current.uid)
            .update(mapOf("interests" to interests, "hasChosenInterests" to true))
        user.value = current.copy(interests = interests, hasChosenInterests = true)
    }

    fun uidOrNull(): String? = user.value?.uid
    fun requireUid(): String = checkNotNull(uidOrNull()) { "Not signed in" }
}
