package uno.lux.autoskola.bank

import kotlin.random.Random

/** A numbered practice exam with the same structure as the official one. */
data class PracticeTest(val number: Int, val questions: List<Question>)

/**
 * Splits the questions available to [category] into numbered practice tests.
 *
 * Each test takes [Section.questionsPerTest] questions from every section, like the real exam.
 * There are just enough tests for every question to appear at least once; sections with fewer
 * questions repeat them in later tests. The split is deterministic, so "Test 7" stays the same
 * test between launches (and across platforms) as long as the bank doesn't change.
 */
fun QuestionBank.practiceTests(category: LicenseCategory): List<PracticeTest> {
    val pools = sections.map { section ->
        val pool = questions
            .filter { it.section == section.id && category in it.groups && it.answers.size > 1 }
            .shuffled(Random(stableSeed("$id/${category.name}/${section.id}")))
        section to pool
    }.filter { (_, pool) -> pool.isNotEmpty() }
    if (pools.isEmpty()) return emptyList()

    val testCount = pools.maxOf { (section, pool) -> ceilDiv(pool.size, section.questionsPerTest) }
    return (0 until testCount).map { index ->
        PracticeTest(
            number = index + 1,
            questions = pools.flatMap { (section, pool) ->
                val perTest = minOf(section.questionsPerTest, pool.size)
                List(perTest) { offset -> pool[(index * perTest + offset) % pool.size] }
            },
        )
    }
}

private fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

// String.hashCode() isn't guaranteed to match across platforms; FNV-1a is.
private fun stableSeed(key: String): Int {
    var hash = 0x811C9DC5.toInt()
    for (char in key) {
        hash = (hash xor char.code) * 0x01000193
    }
    return hash
}
