package com.safeexam.browser.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.dao.ViolationDao
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.db.entity.Violation

@Database(
    entities = [Exam::class, Violation::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun violationDao(): ViolationDao

    companion object {
        const val DATABASE_NAME = "safe_exam_db"

        /**
         * Migrasi dari v1 (tanpa mapel/tanggal/waktu/durasi)
         * ke v2 (dengan kolom baru).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exams ADD COLUMN mapel TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exams ADD COLUMN tanggal TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exams ADD COLUMN waktu TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE exams ADD COLUMN durasi INTEGER NOT NULL DEFAULT 60")
            }
        }
    }
}
