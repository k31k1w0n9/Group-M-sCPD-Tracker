package com.example.scpd_tracker.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val role: AccountRole = AccountRole.Student,
    val email: String = "",
    val password: String = "",
    val error: SignInResult? = null,
    val isSubmitting: Boolean = false,
)

/**
 * Role picker and sign-in. The role picker only chooses which sign-in screen opens; the account's real role
 * comes back from the backend, and a mismatch is refused (R6).
 */
class AuthViewModel(private val auth: AuthRepository, private val saved: SavedStateHandle) : ViewModel() {
    val currentUser: StateFlow<UserProfile?> = auth.currentUser

    private val _state = MutableStateFlow(
        SignInUiState(role = saved.get<String>(KEY_ROLE)?.let(AccountRole::valueOf) ?: AccountRole.Student, email = saved[KEY_EMAIL] ?: ""),
    )
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun selectRole(role: AccountRole) {
        saved[KEY_ROLE] = role.name
        _state.update { it.copy(role = role, error = null) }
    }

    fun onEmail(value: String) {
        saved[KEY_EMAIL] = value
        _state.update { it.copy(email = value, error = null) }
    }

    /** The password is deliberately not saved in SavedStateHandle. */
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val current = _state.value
        if (current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = auth.signIn(current.email, current.password, current.role)
            _state.update {
                if (result is SignInResult.Success) SignInUiState(role = it.role)
                else it.copy(isSubmitting = false, error = result, password = "")
            }
        }
    }

    fun continueAsStudent() = selectRole(AccountRole.Student)

    fun clearForm() = _state.update { it.copy(password = "", error = null) }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
        _state.update { SignInUiState(role = it.role) }
    }

    fun saveInterests(interests: List<String>, onSaved: () -> Unit) {
        viewModelScope.launch {
            auth.saveInterests(interests)
            onSaved()
        }
    }

    private companion object {
        const val KEY_ROLE = "role"
        const val KEY_EMAIL = "email"
    }
}
