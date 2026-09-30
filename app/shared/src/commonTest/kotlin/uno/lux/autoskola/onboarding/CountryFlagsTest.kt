package uno.lux.autoskola.onboarding

import uno.lux.autoskola.bank.QuestionBankInfo
import uno.lux.autoskola.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryFlagsTest {

    @Test
    fun anUnknownCountryHasNoFlag() {
        assertNull(countryFlag("PL"))
    }

    @Test
    fun aCountryCodeIsMatchedWhateverItsCase() {
        assertNotNull(countryFlag("sk"))
        assertNotNull(countryFlag("Sk"))
    }

    @Test
    fun eachCountryHasItsOwnFlag() {
        assertNotEquals(countryFlag("SK"), countryFlag("CZ"))
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
