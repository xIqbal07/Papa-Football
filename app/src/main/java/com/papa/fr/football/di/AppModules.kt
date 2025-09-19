package com.papa.fr.football.di

import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.papa.fr.football.data.remote.LiveEventsApiService
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.data.repository.MatchRepositoryImpl
import com.papa.fr.football.data.repository.SeasonRepositoryImpl
import com.papa.fr.football.data.repository.TeamLogoProvider
import com.papa.fr.football.domain.repository.MatchRepository
import com.papa.fr.football.domain.repository.SeasonRepository
import com.papa.fr.football.domain.usecase.GetLiveMatchesUseCase
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.domain.usecase.GetUpcomingMatchesUseCase
import com.papa.fr.football.presentation.schedule.ScheduleViewModel
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
import org.koin.dsl.module

private const val BASE_HOST = "sofasport.p.rapidapi.com"
private const val HEADER_API_KEY = "X-RapidAPI-Key"
private const val HEADER_API_HOST = "X-RapidAPI-Host"
private const val API_KEY_VALUE = "633787e55amsh33c9302558d5212p1064cdjsncbcd738c7c45"

val networkModule = module {
    single {
        HttpClient(OkHttp) {
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

val dataModule = module {
    single { SeasonApiService(get()) }
    single { TeamApiService(get()) }
    single { LiveEventsApiService(get()) }
    single { TeamLogoProvider(get()) }
    single<SeasonRepository> { SeasonRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get()) }
}

val domainModule = module {
    factory { GetSeasonsUseCase(get()) }
    factory { GetUpcomingMatchesUseCase(get()) }
    factory { GetLiveMatchesUseCase(get()) }
}

val presentationModule = module {
    viewModel { ScheduleViewModel(get(), get(), get()) }
}
