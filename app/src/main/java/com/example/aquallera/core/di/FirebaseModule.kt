package com.example.aquallera.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Firebase singletons injected across the app. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    private const val DATABASE_URL = "https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app"

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL)
}
