package com.spiraclestudios.autoskola.stats

import com.spiraclestudios.autoskola.bank.LicenseCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAttemptTimeTest {

    @Test
    fun defaultsElapsedSecondsToZero() {
        val attempt = TestAttempt(
            id = "a",
            bankId = "sk-sk",
            bankVersion = 1,
            category = LicenseCategory.B,
            kind = AttemptKind.PracticeTest,
            testNumber = 1,
            questionIds = listOf(1, 2, 3),
            startedAt = 1000L,
        )
        assertEquals(0L, attempt.elapsedSeconds)
    }

    @Test
    fun preservesElapsedSecondsThroughEncoding() {
        val attempt = TestAttempt(
            id = "a",
            bankId = "sk-sk",
            bankVersion = 1,
            category = LicenseCategory.B,
            kind = AttemptKind.PracticeTest,
            testNumber = 1,
            questionIds = listOf(1, 2, 3),
            startedAt = 1000L,
            elapsedSeconds = 125L,
        )
        val decoded = TestAttempt.decode(attempt.encode())
        assertEquals(125L, decoded.elapsedSeconds)
    }

    @Test
    fun decodesLegacyJsonWithoutElapsedSecondsAsZero() {
        // Written before attempts recorded how long they took, so it has no "elapsedSeconds". The
        // "checkImmediately" it carries is left over from when an attempt stored the mode it was
        // started in: unknown keys are ignored, so those attempts still load.
        val json =
            """{"id":"a","bankId":"sk-sk","bankVersion":1,"category":"B","kind":"PracticeTest",""" +
                """"testNumber":1,"questionIds":[1,2,3],"checkImmediately":false,"startedAt":1000}"""
        val decoded = TestAttempt.decode(json)
        assertEquals(0L, decoded.elapsedSeconds)
    }

    @Test
    fun officialDurationIsThirtyMinutes() {
        assertEquals(1800L, TestAttempt.OFFICIAL_DURATION_SECONDS)
    }

    @Test
    fun countsElapsedTimeInWholeMinutes() {
        assertEquals(0, elapsedMinutes(0L))
        // Seconds are dropped, not rounded up: a test 23:59 long reads as 23 minutes.
        assertEquals(0, elapsedMinutes(59L))
        assertEquals(1, elapsedMinutes(60L))
        assertEquals(23, elapsedMinutes(23 * 60))
        assertEquals(23, elapsedMinutes(23 * 60 + 59))
        assertEquals(30, elapsedMinutes(1800L))
        // An attempt that predates the clock reads as no time at all, never as a minus.
        assertEquals(0, elapsedMinutes(-30L))
    }
}
