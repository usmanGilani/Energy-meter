package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PowerRecord::class], version = 1, exportSchema = false)
abstract class PowerDatabase : RoomDatabase() {
    abstract val powerDao: PowerDao

    companion object {
        @Volatile
        private var INSTANCE: PowerDatabase? = null

        fun getDatabase(context: Context): PowerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PowerDatabase::class.java,
                    "power_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
