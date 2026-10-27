package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Trip
import com.example.data.model.TripSighting

@Database(
    entities = [Trip::class, TripSighting::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun tripSightingDao(): TripSightingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zim_birds_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
