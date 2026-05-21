package com.archieapps.fles.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 1,
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
        fun create(context: Context): FlesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FlesDatabase::class.java,
                "fles.db"
            ).build()
        }
    }
}
