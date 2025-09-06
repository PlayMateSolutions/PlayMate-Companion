package com.jsramraj.playmatecompanion.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        MemberEntity::class,
        AttendanceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun attendanceDao(): AttendanceDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "playmate_companion_db"
                )
                .addMigrations(
                    // Migration from version 1 to 2: Add attendance table
                )
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
