package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentEntity::class,
        GuardianEntity::class,
        RouteConfigEntity::class,
        AttendanceSessionEntity::class,
        AttendanceRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BusDatabase : RoomDatabase() {

    abstract fun busDao(): BusDao

    companion object {
        @Volatile
        private var INSTANCE: BusDatabase? = null

        fun getInstance(context: Context): BusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusDatabase::class.java,
                    "dowoz_odwoz_bus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
