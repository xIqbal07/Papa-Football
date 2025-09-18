package com.papa.fr.football.di

import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.repository.SeasonRepositoryImpl
import com.papa.fr.football.domain.repository.SeasonRepository
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.presentation.seasons.SeasonsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private const val BASE_HOST = "sofasport.p.rapidapi.com"
private const val HEADER_API_KEY = "X-RapidAPI-Key"
private const val HEADER_API_HOST = "X-RapidAPI-Host"
private const val API_KEY_VALUE = "633787e55amsh33c9302558d5212p1064cdjsncbcd738c7c45"

val networkModule = module {
    single {
        HttpClient(Android) {
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
    single<SeasonRepository> { SeasonRepositoryImpl(get()) }
}

val domainModule = module {
    factory { GetSeasonsUseCase(get()) }
}

val presentationModule = module {
    viewModel { SeasonsViewModel(get()) }
}
