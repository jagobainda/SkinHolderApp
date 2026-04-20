package dev.jagoba.skinholder.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.dataservice.api.ExternalApiService
import dev.jagoba.skinholder.dataservice.api.ItemPrecioApiService
import dev.jagoba.skinholder.dataservice.api.ItemsApiService
import dev.jagoba.skinholder.dataservice.api.LogApiService
import dev.jagoba.skinholder.dataservice.api.RegistroApiService
import dev.jagoba.skinholder.dataservice.api.UserSettingsApiService
import dev.jagoba.skinholder.dataservice.api.SteamPriceApi
import dev.jagoba.skinholder.dataservice.api.UserItemApiService
import dev.jagoba.skinholder.dataservice.repository.ExternalRepository
import dev.jagoba.skinholder.dataservice.repository.ItemPrecioRepository
import dev.jagoba.skinholder.dataservice.repository.ItemsRepository
import dev.jagoba.skinholder.dataservice.repository.LogRepository
import dev.jagoba.skinholder.dataservice.repository.RegistroRepository
import dev.jagoba.skinholder.dataservice.repository.UserItemRepository
import dev.jagoba.skinholder.dataservice.repository.UserRepository
import dev.jagoba.skinholder.dataservice.repository.UserSettingsRepository
import dev.jagoba.skinholder.dataservice.repository.UserRepositoryImpl
import dev.jagoba.skinholder.dataservice.repository.DashboardRepository
import dev.jagoba.skinholder.logic.LoggerService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository = UserRepositoryImpl()

    @Provides
    @Singleton
    fun provideSteamPriceApi(): SteamPriceApi = SteamPriceApi()

    @Provides
    @Singleton
    fun provideItemsRepository(api: ItemsApiService): ItemsRepository = ItemsRepository(api)

    @Provides
    @Singleton
    fun provideUserItemRepository(api: UserItemApiService): UserItemRepository = UserItemRepository(api)

    @Provides
    @Singleton
    fun provideItemPrecioRepository(api: ItemPrecioApiService): ItemPrecioRepository = ItemPrecioRepository(api)

    @Provides
    @Singleton
    fun provideRegistroRepository(api: RegistroApiService): RegistroRepository = RegistroRepository(api)

    @Provides
    @Singleton
    fun provideExternalRepository(api: ExternalApiService): ExternalRepository = ExternalRepository(api)

    @Provides
    @Singleton
    fun provideLogRepository(api: LogApiService): LogRepository = LogRepository(api)

    @Provides
    @Singleton
    fun provideLoggerService(
        logRepository: LogRepository,
        authSessionManager: AuthSessionManager
    ): LoggerService = LoggerService(logRepository, authSessionManager)

    @Provides
    @Singleton
    fun provideUserSettingsRepository(api: UserSettingsApiService): UserSettingsRepository =
        UserSettingsRepository(api)

    @Provides
    @Singleton
    fun provideDashboardRepository(
        registroRepository: RegistroRepository,
        authSessionManager: AuthSessionManager
    ): DashboardRepository = DashboardRepository(registroRepository, authSessionManager)
}
