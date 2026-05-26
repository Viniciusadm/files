package com.archieapps.fles.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import coil3.video.videoFrameMillis
import com.archieapps.fles.BuildConfig
import java.io.File

/**
 * Constrói o "model" que o Coil usa para carregar uma mídia, a partir das fontes
 * possíveis (caminho local, URL de rede ou caminho relativo da API). Para arquivos
 * seguros (criptografados) que ainda não estão em disco, chama [ensureLocal] uma única
 * vez para materializar/descriptografar o arquivo.
 *
 * Retorna um [Uri] (arquivo local) ou uma [String] (URL absoluta de rede), ou `null`
 * quando não há nada para exibir ainda.
 */
@Composable
fun rememberMediaModel(
    source: String?,
    cacheKey: String,
    ensureLocal: (suspend () -> File)? = null,
): Any? {
    val direct = remember(source) { directMediaModel(source) }
    if (direct != null) return direct
    if (ensureLocal == null) return null
    val ensured by produceState<Uri?>(initialValue = null, cacheKey) {
        value = runCatching { Uri.fromFile(ensureLocal()) }.getOrNull()
    }
    return ensured
}

private fun directMediaModel(source: String?): Any? {
    if (source.isNullOrBlank()) return null
    return when {
        source.startsWith("http://") || source.startsWith("https://") -> source
        source.startsWith("file://") || source.startsWith("/") -> {
            val path = source.removePrefix("file://")
            File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) }
        }
        else -> absoluteMediaUrl(source)
    }
}

/** Resolve um caminho relativo da API (ex.: "/api/uploads/x") para URL absoluta. */
fun absoluteMediaUrl(raw: String): String {
    val normalized = raw.removePrefix("/api/").removePrefix("api/").removePrefix("/")
    return BuildConfig.API_BASE_URL.trimEnd('/') + "/" + normalized
}

/** Uri de um arquivo local existente, ou `null`. Útil para thumbnails de vídeo. */
fun localFileUri(localPath: String?): Uri? {
    val path = localPath?.removePrefix("file://") ?: return null
    return File(path).takeIf { it.exists() }?.let { Uri.fromFile(it) }
}

/**
 * Exibe uma mídia via Coil. O Coil decodifica no tamanho da view (downsampling),
 * usa cache de memória/disco e cancela ao sair da tela. Não renderiza nada quando
 * [model] é `null` — o chamador deve prover o fundo/placeholder.
 *
 * @param isVideoFrame quando `true`, extrai o primeiro frame do vídeo apontado por [model].
 * @param fullResolution quando `true`, decodifica em resolução original (ex.: visor com zoom).
 */
@Composable
fun MediaImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isVideoFrame: Boolean = false,
    fullResolution: Boolean = false,
) {
    if (model == null) return
    val context = LocalContext.current
    val request = remember(model, isVideoFrame, fullResolution) {
        ImageRequest.Builder(context)
            .data(model)
            .apply {
                if (isVideoFrame) videoFrameMillis(0)
                if (fullResolution) size(Size.ORIGINAL)
            }
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
