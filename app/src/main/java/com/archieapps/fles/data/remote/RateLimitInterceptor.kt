package com.archieapps.fles.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import kotlin.math.min
import kotlin.random.Random

/**
 * Garante que o módulo de files nunca exponha um 429 ("Too Many Requests") ao usuário.
 *
 * Atua em duas frentes:
 *  - Reativa: quando o servidor responde 429/503, lê o header `Retry-After` (ou usa um
 *    backoff exponencial com jitter) e reenvia a requisição automaticamente, até
 *    [maxRetries] vezes. O erro só vaza se todas as tentativas falharem.
 *  - Proativa (opcional): quando [permitsPerMinute] é informado, espaça as requisições
 *    de saída por um token-bucket simples para ficar abaixo do limite do servidor antes
 *    mesmo de receber um 429.
 *
 * É um application interceptor (registrado depois do AuthInterceptor) para que cada
 * reenvio carregue o header de Authorization atualizado. O bloqueio é feito com
 * Thread.sleep numa thread do dispatcher do OkHttp — a corrotina chamadora apenas fica
 * suspensa aguardando o callback, sem travar a UI.
 */
class RateLimitInterceptor(
    private val permitsPerMinute: Int? = null,
    private val maxRetries: Int = 5,
    private val maxBackoffMs: Long = 30_000L,
) : Interceptor {

    private val minIntervalMs: Long = permitsPerMinute?.let { 60_000L / it.coerceAtLeast(1) } ?: 0L
    private val pacingLock = Any()
    @Volatile private var nextAllowedAt = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        paceOutgoing()
        var response = chain.proceed(chain.request())

        var attempt = 0
        while (response.code in RETRYABLE_CODES && attempt < maxRetries) {
            val waitMs = retryAfterMs(response) ?: backoffMs(attempt)
            response.close()
            if (!sleep(waitMs)) return chain.proceed(chain.request())
            attempt++
            paceOutgoing()
            response = chain.proceed(chain.request())
        }
        return response
    }

    /** Espaça as requisições de saída para nunca ultrapassar [permitsPerMinute]. */
    private fun paceOutgoing() {
        if (minIntervalMs <= 0L) return
        val sleepMs: Long
        synchronized(pacingLock) {
            val now = System.currentTimeMillis()
            val slot = maxOf(now, nextAllowedAt)
            sleepMs = slot - now
            nextAllowedAt = slot + minIntervalMs
        }
        sleep(sleepMs)
    }

    private fun retryAfterMs(response: Response): Long? {
        // Laravel (ThrottleRequests) envia Retry-After em segundos inteiros.
        val seconds = response.header("Retry-After")?.trim()?.toLongOrNull() ?: return null
        return (seconds * 1000L).coerceIn(0L, maxBackoffMs)
    }

    private fun backoffMs(attempt: Int): Long {
        val base = min((1L shl min(attempt, 10)) * 500L, maxBackoffMs)
        return (base * (0.5 + Random.nextDouble() * 0.5)).toLong()
    }

    /** Dorme [ms] respeitando interrupção; retorna false se foi interrompido. */
    private fun sleep(ms: Long): Boolean {
        if (ms <= 0L) return true
        return try {
            Thread.sleep(ms)
            true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private companion object {
        val RETRYABLE_CODES = setOf(429, 503)
    }
}
