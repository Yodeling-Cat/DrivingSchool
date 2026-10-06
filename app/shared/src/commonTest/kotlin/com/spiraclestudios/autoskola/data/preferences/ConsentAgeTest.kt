package com.spiraclestudios.autoskola.data.preferences

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The rule the switches, the analytics gate and the ad gate all read. Pure on purpose: this is the
 * whole of the policy about who may answer, and it is tested with no screen and no repository in the
 * way.
 */
class ConsentAgeTest {

    @Test
    fun anAdultMayAnswerTheSwitches() {
        assertTrue(ConsentAge.mayAnswerSwitches(overConsentAge = true, parentalConsent = false))
    }

    @Test
    fun anUnder16MayNotAnswerThemAlone() {
        assertFalse(ConsentAge.mayAnswerSwitches(overConsentAge = false, parentalConsent = false))
    }

    @Test
    fun anUnder16WhoseParentOrGuardianAgreedMay() {
        assertTrue(ConsentAge.mayAnswerSwitches(overConsentAge = false, parentalConsent = true))
    }

    @Test
    fun anUnansweredQuestionIsNotAnAnswer() {
        assertFalse(ConsentAge.mayAnswerSwitches(overConsentAge = null, parentalConsent = false))
        // And a stored agreement does not stand in for the question: nothing has been asked.
        assertFalse(ConsentAge.mayAnswerSwitches(overConsentAge = null, parentalConsent = true))
    }

    @Test
    fun aStoredYesIsWorthNothingWhileTheQuestionIsUnanswered() {
        // The state an app updated from a version before the question is in: its switches may say yes.
        assertFalse(ConsentAge.effective(requested = true, overConsentAge = null, parentalConsent = false))
        assertFalse(ConsentAge.effective(requested = true, overConsentAge = false, parentalConsent = false))
        assertTrue(ConsentAge.effective(requested = true, overConsentAge = false, parentalConsent = true))
        assertTrue(ConsentAge.effective(requested = true, overConsentAge = true, parentalConsent = false))
    }

    @Test
    fun aRefusedAnswerStaysRefused() {
        assertFalse(ConsentAge.effective(requested = false, overConsentAge = true, parentalConsent = false))
        assertFalse(ConsentAge.effective(requested = false, overConsentAge = null, parentalConsent = true))
    }

    @Test
    fun googleIsToldUnlessTheUserSaidTheyAre16OrOlder() {
        assertTrue(ConsentAge.underAgeOfConsent(overConsentAge = false))
        // Conservative on purpose: the flag comes off on an answer the user gave, never by default.
        assertTrue(ConsentAge.underAgeOfConsent(overConsentAge = null))
        assertFalse(ConsentAge.underAgeOfConsent(overConsentAge = true))
    }
}
