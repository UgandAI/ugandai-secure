package com.ugandai.ugandai.data.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ugandai.ugandai.BuildConfig
import com.ugandai.ugandai.auth.data.AuthTokenStore
import com.ugandai.ugandai.utils.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object UgandAIApiClient {
    @Volatile private var tokenStore: AuthTokenStore? = null

    fun initialize(context: Context) {
        tokenStore = AuthTokenStore(context)
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    authorizationValue(tokenStore?.token())?.let {
                        header("Authorization", it)
                    }
                }.build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL.trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: UgandAIApi by lazy { retrofit.create(UgandAIApi::class.java) }
}

internal fun authorizationValue(token: String?): String? =
    token?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
