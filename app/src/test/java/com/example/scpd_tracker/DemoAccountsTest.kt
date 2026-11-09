package com.example.scpd_tracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoAccountsTest {
    @Test
    fun studentAccountOpensStudentEntry() {
        val result = DemoAccounts.signIn(AccountRole.Student, " Student@scpd.example ", "scpd-demo")
        assertEquals(DemoSignInResult.SignedIn(AccountRole.Student), result)
    }

    @Test
    fun committeeAccountOpensCommitteeEntry() {
        val result = DemoAccounts.signIn(AccountRole.Committee, "committee@scpd.example", "scpd-demo")
        assertEquals(DemoSignInResult.SignedIn(AccountRole.Committee), result)
    }

    @Test
    fun choosingCommitteeDoesNotGrantCommitteeAccessToStudentAccount() {
        val result = DemoAccounts.signIn(AccountRole.Committee, "student@scpd.example", "scpd-demo")
        assertEquals(DemoSignInResult.RoleNotAllowed, result)
    }

    @Test
    fun wrongPasswordDoesNotSignIn() {
        val result = DemoAccounts.signIn(AccountRole.Student, "student@scpd.example", "wrong")
        assertEquals(DemoSignInResult.InvalidCredentials, result)
    }
}
