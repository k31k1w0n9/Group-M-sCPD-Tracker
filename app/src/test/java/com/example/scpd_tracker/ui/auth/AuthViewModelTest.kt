package com.example.scpd_tracker.ui.auth

import androidx.lifecycle.SavedStateHandle
import com.example.scpd_tracker.data.fake.FakeData
import com.example.scpd_tracker.data.fake.TestApp
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.SignInResult
import com.example.scpd_tracker.ui.MainDispatcherRule
import com.example.scpd_tracker.ui.navigation.Routes
import com.example.scpd_tracker.ui.navigation.homeRouteFor
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private fun TestApp.vm() = AuthViewModel(auth, SavedStateHandle())

    @Test
    fun studentSignsIn_navigatesToStudentGraph() = runTest {
        val app = TestApp(backgroundScope)
        val vm = app.vm()
        vm.onEmail(FakeData.STUDENT_1)
        vm.onPassword(FakeData.PASSWORD)
        vm.signIn()
        advanceUntilIdle()
        assertEquals(Routes.STUDENT, homeRouteFor(vm.currentUser.value))
    }

    @Test
    fun committeeAccountChoosingStudent_opensStudentApp() = runTest {
        val app = TestApp(backgroundScope)
        val vm = app.vm()
        vm.selectRole(AccountRole.Student)
        vm.onEmail(FakeData.COMMITTEE_TECH)
        vm.onPassword(FakeData.PASSWORD)
        vm.signIn()
        advanceUntilIdle()
        assertEquals(null, vm.state.value.error)
        assertEquals(AccountRole.Student, vm.currentUser.value?.role)
        assertEquals(Routes.STUDENT, homeRouteFor(vm.currentUser.value))
    }

    @Test
    fun committeeStudentLogin_keepsClubMembership() = runTest {
        val app = TestApp(backgroundScope)
        app.auth.signIn(FakeData.COMMITTEE_TECH, FakeData.PASSWORD, AccountRole.Student)
        assertEquals(listOf("cs-society"), app.auth.currentUser.value?.clubIds)
        assertEquals(Routes.STUDENT, homeRouteFor(app.auth.currentUser.value))
        app.auth.saveInterests(listOf("Career"))
        assertEquals(AccountRole.Student, app.auth.currentUser.value?.role)
        assertEquals(listOf("cs-society"), app.auth.currentUser.value?.clubIds)
    }

    @Test
    fun committeeSignsIn_landsOnCommitteeGraph() = runTest {
        val app = TestApp(backgroundScope)
        val vm = app.vm()
        vm.selectRole(AccountRole.Committee)
        vm.onEmail(FakeData.COMMITTEE_TECH)
        vm.onPassword(FakeData.PASSWORD)
        vm.signIn()
        advanceUntilIdle()
        assertEquals(Routes.COMMITTEE, homeRouteFor(vm.currentUser.value))
    }

    @Test
    fun wrongPassword_showsInvalidCredentials_andClearsPassword() = runTest {
        val app = TestApp(backgroundScope)
        val vm = app.vm()
        vm.onEmail(FakeData.STUDENT_1)
        vm.onPassword("wrong")
        vm.signIn()
        advanceUntilIdle()
        assertEquals(SignInResult.InvalidCredentials, vm.state.value.error)
        assertEquals("", vm.state.value.password)
    }

    @Test
    fun whileSigningIn_buttonDisabled_andSecondTapIgnored() = runTest {
        val app = TestApp(backgroundScope)
        val vm = app.vm()
        vm.onEmail(FakeData.STUDENT_1)
        vm.onPassword(FakeData.PASSWORD)
        vm.signIn()
        runCurrent()
        assertTrue(vm.state.value.isSubmitting)
        vm.signIn() // ignored
        advanceUntilIdle()
        assertEquals(false, vm.state.value.isSubmitting)
    }

    @Test
    fun offline_showsOffline() = runTest {
        val app = TestApp(backgroundScope, online = false)
        val vm = app.vm()
        vm.onEmail(FakeData.STUDENT_1)
        vm.onPassword(FakeData.PASSWORD)
        vm.signIn()
        advanceUntilIdle()
        assertEquals(SignInResult.Offline, vm.state.value.error)
    }

    @Test
    fun newStudentWithoutInterests_goesToInterestsFirst() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent2() // Daniel has not chosen interests yet
        assertEquals(Routes.INTERESTS, homeRouteFor(app.auth.currentUser.value))
        app.auth.saveInterests(listOf("Sports"))
        assertEquals(Routes.STUDENT, homeRouteFor(app.auth.currentUser.value))
    }

    @Test
    fun emailSurvivesRecreation_passwordDoesNot() = runTest {
        val app = TestApp(backgroundScope)
        val saved = SavedStateHandle()
        AuthViewModel(app.auth, saved).apply { onEmail("a@b.c"); onPassword("secret") }
        val restored = AuthViewModel(app.auth, saved)
        assertEquals("a@b.c", restored.state.value.email)
        assertEquals("", restored.state.value.password)
    }
}
