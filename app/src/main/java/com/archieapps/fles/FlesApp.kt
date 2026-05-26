package com.archieapps.fles

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.archieapps.fles.data.local.TokenStorage
import com.archieapps.fles.data.remote.AuthInterceptor
import com.archieapps.fles.data.remote.RateLimitInterceptor
import com.archieapps.fles.di.AppContainer
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

class FlesApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * ImageLoader único do app. Faz o que apps de galeria fazem: decodifica a imagem
     * no tamanho da view (downsampling), guarda em cache de memória e disco, deduplica
     * requisições e cancela o trabalho ao sair da tela. Também extrai frames de vídeo
     * (VideoFrameDecoder) e busca mídias de rede pelo OkHttp com o token de autenticação.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(TokenStorage(this)))
            // Sem pacing proativo aqui: o Coil já limita a concorrência e mantém cache;
            // só reabsorvemos eventuais 429/503 com Retry-After para a thumbnail apenas
            // carregar um instante depois, sem travar a rolagem nem quebrar a imagem.
            .addInterceptor(RateLimitInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
