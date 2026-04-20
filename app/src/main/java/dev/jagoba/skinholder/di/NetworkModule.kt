package dev.jagoba.skinholder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.skinholder.core.AuthInterceptor
import dev.jagoba.skinholder.dataservice.api.AuthApi
import dev.jagoba.skinholder.dataservice.api.ExternalApiService
import dev.jagoba.skinholder.dataservice.api.ItemPrecioApiService
import dev.jagoba.skinholder.dataservice.api.ItemsApiService
import dev.jagoba.skinholder.dataservice.api.LogApiService
import dev.jagoba.skinholder.dataservice.api.RegistroApiService
import dev.jagoba.skinholder.dataservice.api.UserItemApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://shapi.jagoba.dev/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideItemsApiService(retrofit: Retrofit): ItemsApiService {
        return retrofit.create(ItemsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserItemApiService(retrofit: Retrofit): UserItemApiService {
        return retrofit.create(UserItemApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideItemPrecioApiService(retrofit: Retrofit): ItemPrecioApiService {
        return retrofit.create(ItemPrecioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRegistroApiService(retrofit: Retrofit): RegistroApiService {
        return retrofit.create(RegistroApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLogApiService(retrofit: Retrofit): LogApiService {
        return retrofit.create(LogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideExternalApiService(retrofit: Retrofit): ExternalApiService {
        return retrofit.create(ExternalApiService::class.java)
    }
}
