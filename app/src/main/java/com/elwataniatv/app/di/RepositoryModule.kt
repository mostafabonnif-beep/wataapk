package com.elwataniatv.app.di

import android.content.Context
import com.elwataniatv.app.data.local.AppDatabase
import com.elwataniatv.app.data.repository.WataniaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideWataniaRepository(
        database: AppDatabase,
        @ApplicationContext context: Context
    ): WataniaRepository = WataniaRepository(database, context)
}
