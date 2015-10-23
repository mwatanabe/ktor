package org.jetbrains.ktor.http

import org.jetbrains.ktor.util.*
import java.io.*

class ContentDisposition(val disposition: String, val parameters: ValuesMap) {
    val name: String?
        get() = parameters["name"]

    override fun toString(): String {
        return (listOf(disposition) + parameters.entries()
                .flatMap { it.value.map { e -> it.key to e } }
                .map { "${it.first}=${it.second.escapeIfNeeded()}" })
                .joinToString("; ")
    }

    fun withParameter(key: String, value: String) = ContentDisposition(disposition, ValuesMap.build { appendAll(parameters); append(key, value) })
    fun withParameters(newParameters: ValuesMap) = ContentDisposition(disposition, parameters + newParameters)

    private fun String.escapeIfNeeded() = when {
        indexOfAny("\"=;,\\/".toCharArray()) != -1 -> quote()
        else -> this
    }

    companion object {
        val File = ContentDisposition("file", ValuesMap.Empty)
        val Mixed = ContentDisposition("mixed", ValuesMap.Empty)

        fun parse(value: String) = parseHeaderValue(value).let { preParsed ->
            ContentDisposition(preParsed.single().value,
                    parameters = ValuesMap(preParsed.single().params.groupBy({ it.name }, { it.value }), caseInsensitiveKey = true)
            )
        }
    }
}

sealed class PartData(open val dispose: () -> Unit, open val partHeaders: ValuesMap) {
    class FormItem(val value: String, override val dispose: () -> Unit, override val partHeaders: ValuesMap) : PartData(dispose, partHeaders)
    class FileItem(val streamProvider: () -> InputStream, override val dispose: () -> Unit, override val partHeaders: ValuesMap) : PartData(dispose, partHeaders) {
        val originalFileName = contentDisposition?.parameters?.get("filename")
    }

    val contentDisposition: ContentDisposition? by lazy {
        partHeaders[HttpHeaders.ContentDisposition]?.let { ContentDisposition.parse(it) }
    }

    val partName: String?
        get() = contentDisposition?.name

    val contentType: ContentType? by lazy { partHeaders[HttpHeaders.ContentType]?.let { ContentType.parse(it) } }
}

interface MultiPartData {
    val parts: Sequence<PartData>
    // TODO think of possible async methods
}

private fun <T> Iterable<T>.groupBy(key: (T) -> String, value: (T) -> String): Map<String, List<String>> =
        groupBy(key).mapValues { it.value.map(value) }

private fun String.quote() = "\"" + replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\"") + "\""
