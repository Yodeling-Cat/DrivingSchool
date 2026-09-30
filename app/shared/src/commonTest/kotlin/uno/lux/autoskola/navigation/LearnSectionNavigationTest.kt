package uno.lux.autoskola.navigation

import uno.lux.autoskola.learn.LearnSection
import kotlin.test.Test
import kotlin.test.assertEquals

class LearnSectionNavigationTest {

    @Test
    fun everySubjectOpensItsOwnScreen() {
        assertEquals(SignsKey, LearnSection.RoadSigns.key())
        assertEquals(IntersectionsKey, LearnSection.Intersections.key())
        assertEquals(LegislationKey, LearnSection.Legislation.key())
    }

    @Test
    fun noTwoSubjectsShareAScreen() {
        val keys = LearnSection.entries.map { it.key() }

        assertEquals(LearnSection.entries.size, keys.distinct().size)
    }
}
