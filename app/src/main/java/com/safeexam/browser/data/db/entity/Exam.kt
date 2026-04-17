package com.safeexam.browser.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val namaUjian: String,
    val mapel: String,          // Mata Pelajaran
    val tanggal: String,        // Tanggal ujian (dd/MM/yyyy)
    val waktu: String,          // Waktu mulai (HH:mm)
    val durasi: Int,            // Durasi dalam menit
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)
