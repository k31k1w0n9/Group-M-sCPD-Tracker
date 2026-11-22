package com.example.scpd_tracker.model

/** The two roles that use the Android app. The s-CPD office (admin) works in the backend console only. */
enum class AccountRole(val label: String) {
    Student("Student"),
    Committee("Club Committee"),
}

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val role: AccountRole,
    val studentId: String? = null,
    /** Clubs a committee account manages. Empty for students. */
    val clubIds: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    /** False until the student has been through "Make it yours" once. */
    val hasChosenInterests: Boolean = false,
) {
    fun canUse(requestedRole: AccountRole): Boolean = when (requestedRole) {
        AccountRole.Student -> studentId != null
        AccountRole.Committee -> clubIds.isNotEmpty()
    }
}

sealed interface SignInResult {
    data class Success(val user: UserProfile) : SignInResult
    data object InvalidCredentials : SignInResult
    /** The account exists but its role is not the one picked on the role screen. */
    data object RoleNotAllowed : SignInResult
    data object Offline : SignInResult
}
