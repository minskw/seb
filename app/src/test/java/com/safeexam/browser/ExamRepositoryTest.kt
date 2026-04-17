package com.safeexam.browser

import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExamRepositoryTest {

    private lateinit var dao: ExamDao
    private lateinit var repository: ExamRepository

    private fun makeExam(
        name: String = "Ujian Matematika",
        mapel: String = "Matematika",
        tanggal: String = "01/06/2025",
        waktu: String = "08:00",
        durasi: Int = 90,
        url: String = "https://docs.google.com/forms/d/test"
    ) = Exam(namaUjian = name, mapel = mapel, tanggal = tanggal,
              waktu = waktu, durasi = durasi, url = url)

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = ExamRepository(dao)
    }

    @Test
    fun `saveExam calls insertExam on dao`() = runTest {
        val exam = makeExam()
        coEvery { dao.insertExam(exam) } returns 1L
        repository.saveExam(exam)
        coVerify { dao.insertExam(exam) }
    }

    @Test
    fun `isValidGoogleFormUrl returns true for https google form`() {
        assertTrue(repository.isValidGoogleFormUrl("https://docs.google.com/forms/d/abc"))
    }

    @Test
    fun `isValidGoogleFormUrl returns true for http google form`() {
        assertTrue(repository.isValidGoogleFormUrl("http://docs.google.com/forms/d/abc"))
    }

    @Test
    fun `isValidGoogleFormUrl returns false for non-google URL`() {
        assertFalse(repository.isValidGoogleFormUrl("https://example.com/form"))
    }

    @Test
    fun `isValidGoogleFormUrl returns false for empty string`() {
        assertFalse(repository.isValidGoogleFormUrl(""))
    }
}
