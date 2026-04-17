package com.safeexam.browser

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import com.safeexam.browser.ui.addexam.AddExamViewModel
import com.safeexam.browser.ui.addexam.SaveState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExamViewModelTest {

    @get:Rule val instantTask = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ExamRepository
    private lateinit var viewModel: AddExamViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.isValidGoogleFormUrl(any()) } answers {
            firstArg<String>().startsWith("https://docs.google.com/forms/")
        }
        coEvery { repository.saveExam(any()) } returns 1L
        viewModel = AddExamViewModel(repository)
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `saveExam with empty name emits Error`() = runTest {
        viewModel.saveExam("", "Matematika", "01/06/2025", "08:00", "90",
            "https://docs.google.com/forms/d/abc")
        assertTrue(viewModel.saveState.value is SaveState.Error)
    }

    @Test
    fun `saveExam with empty mapel emits Error`() = runTest {
        viewModel.saveExam("Ujian", "", "01/06/2025", "08:00", "90",
            "https://docs.google.com/forms/d/abc")
        assertTrue(viewModel.saveState.value is SaveState.Error)
    }

    @Test
    fun `saveExam with invalid durasi emits Error`() = runTest {
        viewModel.saveExam("Ujian", "Matematika", "01/06/2025", "08:00", "abc",
            "https://docs.google.com/forms/d/abc")
        assertTrue(viewModel.saveState.value is SaveState.Error)
    }

    @Test
    fun `saveExam with invalid URL emits Error`() = runTest {
        viewModel.saveExam("Ujian", "Matematika", "01/06/2025", "08:00", "90",
            "https://example.com/form")
        val state = viewModel.saveState.value
        assertTrue(state is SaveState.Error)
    }

    @Test
    fun `saveExam with valid inputs emits Success`() = runTest {
        viewModel.saveExam("Ujian", "Matematika", "01/06/2025", "08:00", "90",
            "https://docs.google.com/forms/d/abc")
        advanceUntilIdle()
        assertTrue(viewModel.saveState.value is SaveState.Success)
    }

    @Test
    fun `resetState sets Idle`() = runTest {
        viewModel.saveExam("", "", "", "", "", "")
        viewModel.resetState()
        assertEquals(SaveState.Idle, viewModel.saveState.value)
    }
}
