package com.archieapps.fles.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        ChannelEntity::class,
        MessageEntity::class,
        ListItemEntity::class,
        AttachmentEntity::class,
        SyncQueueEntity::class,
        SyncStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FlesDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun listItemDao(): ListItemDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        @Volatile private var instance: FlesDatabase? = null

        fun create(context: Context): FlesDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlesDatabase::class.java,
                    "fles.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .enableMultiInstanceInvalidation()
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE channels ADD COLUMN isSecure INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_isSecure ON channels(isSecure)")
            }
        }
    }
}
