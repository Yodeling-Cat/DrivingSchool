package com.spiraclestudios.autoskola.data.banks

import com.spiraclestudios.autoskola.bank.Question
import com.spiraclestudios.autoskola.bank.QuestionBank
import com.spiraclestudios.autoskola.bank.QuestionBankInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The question banks, each read once and then kept in memory. */
class QuestionBankRepository(private val dataSource: QuestionBankDataSource) {
    private val mutex = Mutex()
    private var index: List<QuestionBankInfo>? = null
    private val banks = mutableMapOf<String, QuestionBank>()

    suspend fun index(): List<QuestionBankInfo> = mutex.withLock {
        index ?: dataSource.readIndex().also { index = it }
    }

    /** The latest version of bank [id], or `null` if there's no such bank. */
    suspend fun latestBank(id: String): QuestionBank? {
        val version = index().firstOrNull { it.id == id }?.version ?: return null
        return bank(id, version)
    }

    /**
     * A specific version, e.g. to show an old attempt; older versions stay bundled. Only banks that
     * were read successfully are remembered: a read that failed or was canceled is tried again
     * next time, so a hiccup while loading can't leave the exam "missing" for the whole process.
     */
    suspend fun bank(id: String, version: Int): QuestionBank? = mutex.withLock {
        val key = "$id-v$version"
        banks[key] ?: dataSource.readBank(id, version)?.also { banks[key] = it }
    }

    /** URI of the image of [question], for the image loader, or `null` for text-only questions. */
    fun imageUri(bank: QuestionBank, question: Question): String? = question.image?.let { imageUri(bank, it) }

    /**
     * URI of [image], a path relative to the bank's image directory, for the image loader. A
     * question's picture and a situation's drawing are addressed the same way.
     */
    fun imageUri(bank: QuestionBank, image: String): String = dataSource.imageUri(bank.imageDir, image)

    /** URIs of the pictures [question]'s answers are, in their order, or `null` when they are words. */
    fun answerImageUris(bank: QuestionBank, question: Question): List<String>? =
        question.answerImages?.map { imageUri(bank, it) }
}
