package com.safeexam.browser.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.databinding.ItemExamBinding

class ExamListAdapter(
    private val onExamClick: (Exam) -> Unit,
    private val onDeleteClick: (Exam) -> Unit
) : ListAdapter<Exam, ExamListAdapter.ExamViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val binding = ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExamViewHolder(
        private val binding: ItemExamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exam: Exam) {
            binding.tvExamName.text      = exam.namaUjian
            binding.tvMapel.text         = exam.mapel
            binding.tvTanggal.text       = exam.tanggal
            binding.tvWaktuDurasi.text   = "${exam.waktu}  •  ${exam.durasi} menit"
            binding.tvExamUrl.text       = exam.url

            binding.root.setOnClickListener { onExamClick(exam) }
            binding.btnDelete.setOnClickListener { onDeleteClick(exam) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Exam>() {
            override fun areItemsTheSame(old: Exam, new: Exam) = old.id == new.id
            override fun areContentsTheSame(old: Exam, new: Exam) = old == new
        }
    }
}
