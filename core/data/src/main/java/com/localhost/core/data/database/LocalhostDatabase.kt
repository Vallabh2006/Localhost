package com.localhost.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProjectEntity::class,
        LogEntryEntity::class,
        MetricSampleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LocalhostDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun logDao(): LogDao
    abstract fun metricsDao(): MetricsDao
}
