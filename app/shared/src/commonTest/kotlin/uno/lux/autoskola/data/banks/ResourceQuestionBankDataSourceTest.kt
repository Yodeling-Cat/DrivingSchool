package uno.lux.autoskola.data.banks

import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResourceQuestionBankDataSourceTest {

    @Test
    fun readOrNullKeepsTheResult() = runTest {
        assertEquals(7, readOrNull { 7 })
    }

    @Test
    fun readOrNullAnswersNullWhenTheReadFails() = runTest {
        assertNull(readOrNull<Int> { throw IllegalStateException("no such file") })
    }

    @Test
    fun readOrNullRethrowsCancellation() = runTest {
        // A cancelled read (its caller was restarted or went away) is not a bank that isn't
        // bundled. Treating it as one shows the user the exam wizard with no way past it, because
        // the exam they already chose looks like it no longer exists.
        assertFailsWith<CancellationException> {
            readOrNull<Int> { throw CancellationException("cancelled") }
        }
    }
}
