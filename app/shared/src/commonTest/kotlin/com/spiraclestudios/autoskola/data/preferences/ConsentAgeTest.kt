package com.spiraclestudios.autoskola.data.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rule the switches, the analytics gate and the ad gate all read. Pure on purpose: this is the
 * whole of the policy about who may answer, and it is tested with no screen and no repository in the
 * way.
 */
class ConsentAgeTest {

    // --- crash reports and statistics: the line is 16 ----------------------

    @Test
    fun anAdultMayAnswerTheSwitches() {
        assertTrue(ConsentAge.mayAnswerSwitches(band = AgeBand.Adult, parentalConsent = false))
    }

    @Test
    fun a16To17YearOldMayAnswerThemAlone() {
        assertTrue(ConsentAge.mayAnswerSwitches(band = AgeBand.From16To17, parentalConsent = false))
    }

    @Test
    fun anUnder16MayNotAnswerThemAlone() {
        assertFalse(ConsentAge.mayAnswerSwitches(band = AgeBand.Under16, parentalConsent = false))
    }

    @Test
    fun anUnder16WhoseParentOrGuardianAgreedMay() {
        assertTrue(ConsentAge.mayAnswerSwitches(band = AgeBand.Under16, parentalConsent = true))
    }

    @Test
    fun anUnansweredQuestionIsNotAnAnswer() {
        assertFalse(ConsentAge.mayAnswerSwitches(band = null, parentalConsent = false))
        // And a stored agreement does not stand in for the question: nothing has been asked.
        assertFalse(ConsentAge.mayAnswerSwitches(band = null, parentalConsent = true))
    }

    @Test
    fun aStoredYesIsWorthNothingWhileTheQuestionIsUnanswered() {
        // The state an app updated from a version before the question is in: its switches may say yes.
        assertFalse(ConsentAge.effective(requested = true, band = null, parentalConsent = false))
        assertFalse(ConsentAge.effective(requested = true, band = AgeBand.Under16, parentalConsent = false))
        assertTrue(ConsentAge.effective(requested = true, band = AgeBand.Under16, parentalConsent = true))
        assertTrue(ConsentAge.effective(requested = true, band = AgeBand.From16To17, parentalConsent = false))
        assertTrue(ConsentAge.effective(requested = true, band = AgeBand.Adult, parentalConsent = false))
    }

    @Test
    fun aRefusedAnswerStaysRefused() {
        assertFalse(ConsentAge.effective(requested = false, band = AgeBand.Adult, parentalConsent = false))
        assertFalse(ConsentAge.effective(requested = false, band = null, parentalConsent = true))
    }

    // --- personalized ads: the line is 18 ----------------------------------

    @Test
    fun onlyAnAdultMayPersonalizeAds() {
        assertTrue(ConsentAge.mayPersonalizeAds(AgeBand.Adult))
        assertFalse(ConsentAge.mayPersonalizeAds(AgeBand.From16To17))
        assertFalse(ConsentAge.mayPersonalizeAds(AgeBand.Under16))
        assertFalse(ConsentAge.mayPersonalizeAds(null))
    }

    @Test
    fun aStoredYesToPersonalizingIsWorthNothingUnder18() {
        // Whatever was stored — by an older version, or while a parent or guardian's agreement made
        // the other switches answerable — a yes only takes effect for an adult.
        assertTrue(ConsentAge.effectivePersonalizedAds(requested = true, band = AgeBand.Adult))
        assertFalse(ConsentAge.effectivePersonalizedAds(requested = true, band = AgeBand.From16To17))
        assertFalse(ConsentAge.effectivePersonalizedAds(requested = true, band = AgeBand.Under16))
        assertFalse(ConsentAge.effectivePersonalizedAds(requested = true, band = null))
        assertFalse(ConsentAge.effectivePersonalizedAds(requested = false, band = AgeBand.Adult))
    }

    // --- the band itself ----------------------------------------------------

    @Test
    fun aBandRisesOnlyToAHigherOne() {
        assertTrue(AgeBand.Under16.rises(AgeBand.From16To17))
        assertTrue(AgeBand.Under16.rises(AgeBand.Adult))
        assertTrue(AgeBand.From16To17.rises(AgeBand.Adult))
        assertFalse(AgeBand.Adult.rises(AgeBand.From16To17))
        assertFalse(AgeBand.From16To17.rises(AgeBand.From16To17))
    }

    @Test
    fun aBandIsReadBackByTheNameItWasStoredUnder() {
        AgeBand.entries.forEach { assertEquals(it, AgeBand.fromName(it.name)) }
        assertNull(AgeBand.fromName(null))
        // A name a later version wrote and this one does not know reads as nothing answered, which is
        // the safe direction: the question is asked again.
        assertNull(AgeBand.fromName("Pensioner"))
    }
}
