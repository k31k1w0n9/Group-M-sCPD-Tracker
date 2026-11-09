package com.example.scpd_tracker

internal enum class AccountRole(val label: String) {
    Student("Student"),
    Committee("Club Committee"),
}

internal sealed interface DemoSignInResult {
    data class SignedIn(val role: AccountRole) : DemoSignInResult
    data object RoleNotAllowed : DemoSignInResult
    data object InvalidCredentials : DemoSignInResult
}

internal object DemoAccounts {
    const val password = "scpd-demo"

    fun email(role: AccountRole): String = when (role) {
        AccountRole.Student -> "student@scpd.example"
        AccountRole.Committee -> "committee@scpd.example"
    }

    fun signIn(role: AccountRole, email: String, password: String): DemoSignInResult {
        val accountRole = AccountRole.entries.firstOrNull {
            email(it).equals(email.trim(), ignoreCase = true)
        }
        if (accountRole == null || password != this.password) {
            return DemoSignInResult.InvalidCredentials
        }
        if (role != accountRole) return DemoSignInResult.RoleNotAllowed
        return DemoSignInResult.SignedIn(accountRole)
    }
}
