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
import dev.jagoba.skinholder.dataservice.api.UserSettingsApiService
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideItemsApiService(retrofit: Retrofit): ItemsApiService = retrofit.create(ItemsApiService::class.java)

    @Provides
    @Singleton
    fun provideUserItemApiService(retrofit: Retrofit): UserItemApiService =
        retrofit.create(UserItemApiService::class.java)

    @Provides
    @Singleton
    fun provideItemPrecioApiService(retrofit: Retrofit): ItemPrecioApiService =
        retrofit.create(ItemPrecioApiService::class.java)

    @Provides
    @Singleton
    fun provideRegistroApiService(retrofit: Retrofit): RegistroApiService =
        retrofit.create(RegistroApiService::class.java)

    @Provides
    @Singleton
    fun provideLogApiService(retrofit: Retrofit): LogApiService = retrofit.create(LogApiService::class.java)

    @Provides
    @Singleton
    fun provideExternalApiService(retrofit: Retrofit): ExternalApiService =
        retrofit.create(ExternalApiService::class.java)

    @Provides
    @Singleton
    fun provideUserSettingsApiService(retrofit: Retrofit): UserSettingsApiService =
        retrofit.create(UserSettingsApiService::class.java)
}
