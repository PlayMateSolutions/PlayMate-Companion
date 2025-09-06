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
                    object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS `attendance` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `memberId` INTEGER NOT NULL,
                                `date` TEXT NOT NULL,
                                `checkInTime` TEXT NOT NULL,
                                `checkOutTime` TEXT,
                                `membershipStatus` TEXT NOT NULL,
                                `notes` TEXT NOT NULL,
                                `synced` INTEGER NOT NULL,
                                FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                                )
                            """.trimIndent())
                            
                            // Create an index on memberId for better query performance
                            db.execSQL(
                                "CREATE INDEX IF NOT EXISTS `index_attendance_memberId` ON `attendance` (`memberId`)"
                            )
                        }
                    }
                )
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
