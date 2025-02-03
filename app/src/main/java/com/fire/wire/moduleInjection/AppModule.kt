package com.fire.wire.moduleInjection

import android.content.Context
import com.fire.wire.data.APIClient
import com.fire.wire.data.XMLClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideWebService(@ApplicationContext context: Context) = APIClient(context.applicationContext).apiEndPoint

    @Provides
    @Singleton
    fun provideWebXMLService(@ApplicationContext context: Context) = XMLClient(context.applicationContext).newsEndPoints







}