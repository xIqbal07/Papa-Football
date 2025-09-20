package com.papa.fr.football.di

import androidx.room.Room
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.papa.fr.football.common.league.LeagueCatalog
import com.papa.fr.football.common.league.StaticLeagueCatalog
import com.papa.fr.football.data.bootstrap.DataBootstrapper
import com.papa.fr.football.data.bootstrap.MatchPrefetchQueue
import com.papa.fr.football.data.local.database.PapaFootballDatabase
import com.papa.fr.football.data.remote.ApiRateLimiter
import com.papa.fr.football.data.remote.LiveEventsApiService
import com.papa.fr.football.data.remote.RateLimitRule
import com.papa.fr.football.data.remote.RetryingCallExecutor
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.data.repository.MatchRepositoryImpl
import com.papa.fr.football.data.repository.SeasonRepositoryImpl
import com.papa.fr.football.data.repository.TeamLogoProvider
import com.papa.fr.football.data.repository.TeamRepositoryImpl
import com.papa.fr.football.data.repository.UserPreferencesRepositoryImpl
import com.papa.fr.football.domain.repository.MatchRepository
import com.papa.fr.football.domain.repository.SeasonRepository
import com.papa.fr.football.domain.repository.TeamRepository
import com.papa.fr.football.domain.repository.UserPreferencesRepository
import com.papa.fr.football.domain.usecase.GetLiveMatchesUseCase
import com.papa.fr.football.domain.usecase.GetRecentMatchesUseCase
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.domain.usecase.GetUpcomingMatchesUseCase
import com.papa.fr.football.presentation.schedule.ScheduleViewModel
import com.papa.fr.football.presentation.signin.SignInViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val BASE_HOST = "sofasport.p.rapidapi.com"
private const val HEADER_API_KEY = "X-RapidAPI-Key"
private const val HEADER_API_HOST = "X-RapidAPI-Host"
private const val API_KEY_VALUE = "633787e55amsh33c9302558d5212p1064cdjsncbcd738c7c45"

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            expectSuccess = true
            engine {
                addInterceptor(
                    ChuckerInterceptor.Builder(androidContext())
                        .collector(ChuckerCollector(androidContext()))
                        .maxContentLength(250_000L)
                        .alwaysReadResponseBody(true)
                        .build()
                )
            }
            install(DefaultRequest) {
                url {
                    protocol = URLProtocol.HTTPS
                    host = BASE_HOST
                }
                header(HEADER_API_KEY, API_KEY_VALUE)
                header(HEADER_API_HOST, BASE_HOST)
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                    }
                )
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}

val commonModule = module {
    single<LeagueCatalog> { StaticLeagueCatalog() }
}

val dataModule = module {
    single(named("seasonApiRateLimiter")) {
        ApiRateLimiter(
            mapOf(
                SeasonApiService.RATE_LIMIT_KEY_UNIQUE_TOURNAMENT_SEASONS to RateLimitRule(minIntervalMillis = 100),
                SeasonApiService.RATE_LIMIT_KEY_SEASON_EVENTS to RateLimitRule(minIntervalMillis = 100),
            ),
        )
    }
    single {
        Room.databaseBuilder(
            androidContext(),
            PapaFootballDatabase::class.java,
            "papa_football.db",
        ).fallbackToDestructiveMigration().build()
    }
    single { get<PapaFootballDatabase>().seasonDao() }
    single { get<PapaFootballDatabase>().matchDao() }
    single { get<PapaFootballDatabase>().liveMatchDao() }
    single { get<PapaFootballDatabase>().requestRetryDao() }
    single { get<PapaFootballDatabase>().matchPrefetchDao() }
    single { get<PapaFootballDatabase>().userPreferencesDao() }
    single { RetryingCallExecutor(get()) }
    single { SeasonApiService(get(), get(named("seasonApiRateLimiter")), get()) }
    single { TeamApiService(get(), get()) }
    single { LiveEventsApiService(get(), get()) }
    single { TeamLogoProvider(get()) }
    single<SeasonRepository> { SeasonRepositoryImpl(get(), get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get(), get(), get()) }
    single<TeamRepository> { TeamRepositoryImpl(get(), get()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }
    single {
        MatchPrefetchQueue(
            matchRepository = get(),
            matchPrefetchDao = get(),
        ).apply { start() }
    }
    single { DataBootstrapper(get(), get(), get(), get()) }
}

val domainModule = module {
    factory { GetSeasonsUseCase(get()) }
    factory { GetUpcomingMatchesUseCase(get()) }
    factory { GetRecentMatchesUseCase(get()) }
    factory { GetLiveMatchesUseCase(get()) }
}

val presentationModule = module {
    viewModel { ScheduleViewModel(get(), get(), get(), get(), get()) }
    viewModel { SignInViewModel(get(), get(), get()) }
}
