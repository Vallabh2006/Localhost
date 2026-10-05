package com.localhost.core.data

import android.content.Context
import androidx.room.Room
import com.localhost.core.data.database.LocalhostDatabase
import com.localhost.core.data.database.LogDao
import com.localhost.core.data.database.MetricsDao
import com.localhost.core.data.database.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocalhostDatabase {
        return Room.databaseBuilder(
            context,
            LocalhostDatabase::class.java,
            "localhost.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideProjectDao(database: LocalhostDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideLogDao(database: LocalhostDatabase): LogDao = database.logDao()

    @Provides
    fun provideMetricsDao(database: LocalhostDatabase): MetricsDao = database.metricsDao()
}
