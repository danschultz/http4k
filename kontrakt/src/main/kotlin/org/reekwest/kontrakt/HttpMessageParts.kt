package org.reekwest.kontrakt

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues
import org.reekwest.http.core.queries
import org.reekwest.http.core.query
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

typealias QueryLens<T> = Lens<Request, T>

typealias HeaderLens<T> = Lens<Request, T>

typealias PathLens<T> = Lens<String, T>

object Query : BiDiLensSpec<Request, String, String>("query",
    Get { name, target -> target.queries(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.query(name, next) }) }
)

object Header : BiDiLensSpec<HttpMessage, String, String>("header",
    Get { name, target -> target.headerValues(name).map { it ?: "" } },
    Set { name, values, target -> values.fold(target, { m, next -> m.header(name, next) }) }
) {
    object Common {
        val CONTENT_TYPE = Header.map(::ContentType, { it.value }).optional("Content-Type")
    }
}

object Cookies : BiDiLensSpec<Request, Cookie, Cookie>("cookie",
    Get { name, target -> target.cookies().filter { it.name == name } },
    Set { _, values, target -> values.fold(target, { m, (name, value) -> m.cookie(name, value) }) }
)

open class PathSpec<MID, out OUT>(private val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> = delegate.required(name, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))
}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun fixed(name: String) = of(name)
}

fun Path.int() = map(String::toInt)
fun Path.long() = map(String::toLong)
fun Path.double() = map(String::toDouble)
fun Path.float() = map(String::toFloat)
fun Path.boolean() = map(::safeBooleanFrom)
fun Path.localDate() = map(LocalDate::parse)
fun Path.dateTime() = map(LocalDateTime::parse)
fun Path.zonedDateTime() = map(ZonedDateTime::parse)
fun Path.uuid() = map(UUID::fromString)
