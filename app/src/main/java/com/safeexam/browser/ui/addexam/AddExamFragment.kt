package com.safeexam.browser.ui.addexam

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.safeexam.browser.databinding.FragmentAddExamBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddExamFragment : Fragment() {

    private var _binding: FragmentAddExamBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddExamViewModel by viewModels()
    private val args: AddExamFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill URL if coming from QR scanner
        args.prefilledUrl?.let { url ->
            if (url.isNotEmpty()) binding.etUrl.setText(url)
        }

        setupDatePicker()
        setupTimePicker()
        setupObservers()
        setupListeners()
    }

    private fun setupDatePicker() {
        binding.etTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    binding.etTanggal.setText(
                        String.format("%02d/%02d/%04d", day, month + 1, year)
                    )
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        // Also open picker when the TextInputLayout end icon is tapped
        binding.tilTanggal.setEndIconOnClickListener { binding.etTanggal.performClick() }
    }

    private fun setupTimePicker() {
        binding.etWaktu.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    binding.etWaktu.setText(String.format("%02d:%02d", hour, minute))
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
        binding.tilWaktu.setEndIconOnClickListener { binding.etWaktu.performClick() }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        is SaveState.Idle    -> setLoading(false)
                        is SaveState.Saving  -> setLoading(true)
                        is SaveState.Success -> {
                            setLoading(false)
                            Toast.makeText(
                                requireContext(),
                                "Ujian berhasil disimpan",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                        is SaveState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            viewModel.saveExam(
                name    = binding.etName.text.toString(),
                mapel   = binding.etMapel.text.toString(),
                tanggal = binding.etTanggal.text.toString(),
                waktu   = binding.etWaktu.text.toString(),
                durasi  = binding.etDurasi.text.toString(),
                url     = binding.etUrl.text.toString()
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
