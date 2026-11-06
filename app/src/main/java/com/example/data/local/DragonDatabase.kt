package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VideoEntity::class, PlaylistEntity::class],
    version = 3,
    exportSchema = false
)
abstract class DragonDatabase : RoomDatabase() {
    abstract fun dragonDao(): DragonDao

    companion object {
        @Volatile
        private var INSTANCE: DragonDatabase? = null

        fun getInstance(context: Context): DragonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DragonDatabase::class.java,
                    "dragon_video_player.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
