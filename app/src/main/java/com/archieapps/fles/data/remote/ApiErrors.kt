package com.archieapps.fles.data.remote

import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

class ApiException(
    val statusCode: Int?,
    override val message: String
) : RuntimeException(message)

fun Throwable.toFriendlyMessage(): String {
    return when (this) {
        is ApiException -> message
        is HttpException -> friendlyHttpMessage(code(), response()?.errorBody()?.string())
        is SocketTimeoutException -> "Sem conexão com o servidor."
        is IOException -> "Sem conexão com o servidor."
        else -> message?.takeIf { it.isNotBlank() } ?: "Não foi possível concluir a operação"
    }
}

private fun friendlyHttpMessage(code: Int, body: String?): String {
    val serverMessage = body?.let(::extractMessage)
    return when (code) {
        400 -> serverMessage ?: "Dados inválidos."
        401 -> "Sessão expirada. Entre novamente."
        403 -> "Você não tem permissão para esta ação."
        404 -> serverMessage ?: "Conteúdo não encontrado."
        409 -> serverMessage ?: "Reenviando o arquivo…"
        413 -> serverMessage ?: "Cada arquivo pode ter no máximo 1 GB."
        422 -> serverMessage ?: "Revise os dados informados."
        in 500..599 -> "Erro no servidor. Tente novamente."
        else -> serverMessage ?: "Erro na requisição."
    }
}

private fun extractMessage(body: String): String? {
    val match = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)
    return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
