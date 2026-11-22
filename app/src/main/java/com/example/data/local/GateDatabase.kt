package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedQuestionEntity::class,
        AttemptEntity::class,
        StudyProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GateDatabase : RoomDatabase() {
    abstract fun gateDao(): GateDao

    companion object {
        @Volatile
        private var INSTANCE: GateDatabase? = null

        fun getInstance(context: Context): GateDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GateDatabase::class.java,
                    "gate_ec_2026.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
