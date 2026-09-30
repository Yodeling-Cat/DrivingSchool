package uno.lux.autoskola.onboarding

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.bank.QuestionBankInfo
import uno.lux.autoskola.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryFlagsTest {

    @Test
    fun anUnknownCountryHasNoFlag() {
        assertNull(countryFlag("AT"))
    }

    @Test
    fun aCountryCodeIsMatchedWhateverItsCase() {
        assertNotNull(countryFlag("sk"))
        assertNotNull(countryFlag("Sk"))
    }

    @Test
    fun everyCountryWeDrawHasItsOwnFlag() {
        assertTrue(countriesWithFlags.isNotEmpty())
        val flags = countriesWithFlags.map { countryFlag(it) }
        assertEquals(countriesWithFlags.size, flags.distinct().size, "two countries share one flag")
    }

    /** The card draws a flag at the vector's own size, so a squashed flag would show up here. */
    @Test
    fun aFlagIsFortyDpTallAndAsWideAsItsOwnProportions() {
        assertEquals(DpSize(60.dp, 40.dp), countryFlag("SK")!!.size, "Slovakia's flag is 3:2")
        assertEquals(DpSize(60.dp, 40.dp), countryFlag("CZ")!!.size, "Czechia's flag is 3:2")
        assertEquals(DpSize(80.dp, 40.dp), countryFlag("HU")!!.size, "Hungary's flag is 2:1")
        assertEquals(DpSize(64.dp, 40.dp), countryFlag("PL")!!.size, "Poland's flag is 8:5")
    }

    /**
     * A country without a flag would fall back to drawing its two-letter code, so catch a new
     * country being offered before anyone notices the card looks wrong.
     */
    @Test
    fun everyCountryTheWizardOffersHasAFlag() {
        val offered = ExamSelectionUiState(
            banks = emptyList<QuestionBankInfo>(),
            step = ExamSelectionStep.ChooseCountry,
            language = AppLanguage.English,
        ).countryOptions

        assertTrue(offered.isNotEmpty())
        for (option in offered) {
            assertNotNull(countryFlag(option.country), "no flag drawn for ${option.country}")
        }
    }
}

private val ImageVector.size: DpSize get() = DpSize(defaultWidth, defaultHeight)
