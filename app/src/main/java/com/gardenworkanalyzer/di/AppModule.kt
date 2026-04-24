package com.gardenworkanalyzer.di

import android.content.ContentResolver
import android.content.Context
import com.gardenworkanalyzer.BuildConfig
import com.gardenworkanalyzer.data.compressor.ImageCompressor
import com.gardenworkanalyzer.data.network.HuggingFaceApiService
import com.gardenworkanalyzer.data.network.RetryInterceptor
import com.gardenworkanalyzer.data.repository.GardenAnalysisRepository
import com.gardenworkanalyzer.data.repository.GardenAnalysisRepositoryImpl
import com.gardenworkanalyzer.data.repository.ImageRepository
import com.gardenworkanalyzer.data.repository.ImageRepositoryImpl
import com.gardenworkanalyzer.domain.usecase.ManageImageCollectionUseCase
import com.gardenworkanalyzer.domain.usecase.SequenceImagesUseCase
import com.gardenworkanalyzer.ui.permission.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val HF_BASE_URL = "https://api-inference.huggingface.co/"
    private const val TIMEOUT_SECONDS = 60L

    @Provides
    @Singleton
    fun provideImageRepository(): ImageRepository = ImageRepositoryImpl()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun provideManageImageCollectionUseCase(
        imageRepository: ImageRepository,
        contentResolver: ContentResolver
    ) = ManageImageCollectionUseCase(imageRepository, contentResolver)

    @Provides
    fun provideSequenceImagesUseCase(imageRepository: ImageRepository) =
        SequenceImagesUseCase(imageRepository)

    @Provides
    @Singleton
    fun provideImageCompressor(contentResolver: ContentResolver) =
        ImageCompressor(contentResolver)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHuggingFaceApiService(okHttpClient: OkHttpClient): HuggingFaceApiService =
        Retrofit.Builder()
            .baseUrl(HF_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HuggingFaceApiService::class.java)

    @Provides
    @Singleton
    fun provideGardenAnalysisRepository(
        huggingFaceApiService: HuggingFaceApiService,
        imageCompressor: ImageCompressor
    ): GardenAnalysisRepository =
        GardenAnalysisRepositoryImpl(huggingFaceApiService, imageCompressor, BuildConfig.HF_API_TOKEN)

    @Provides
    fun providePermissionManager() = PermissionManager()
}
