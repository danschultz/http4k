package org.http4k.client

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.Test
import java.nio.ByteBuffer

abstract class HttpClientContract(serverConfig: (Int) -> ServerConfig,
                                  val client: HttpHandler,
                                  private val timeoutClient: HttpHandler) : AbstractHttpClientContract(serverConfig) {

    @Test
    fun `can forward response body to another request`() {
        val response = client(Request(GET, "http://localhost:$port/stream"))
        val echoResponse = client(Request(POST, "http://localhost:$port/echo").body(response.body))
        echoResponse.bodyString().shouldMatch(equalTo("stream"))
    }

    @Test
    fun `supports gzipped content`() {
        val asServer = ServerFilters.GZip().then { Response(Status.OK).body("hello") }.asServer(SunHttp())
        asServer.start()
        val client = ApacheClient()

        val request = Request(Method.GET, "http://localhost:8000").header("accept-encoding", "gzip")
        client(request)
        client(request)
        client(request)
        asServer.stop()
    }

    @Test
    fun `can make call`() {
        val response = client(Request(POST, "http://localhost:$port/someUri")
            .query("query", "123")
            .header("header", "value").body("body"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("uri"), equalTo("/someUri?query=123"))
        assertThat(response.header("query"), equalTo("123"))
        assertThat(response.header("header"), equalTo("value"))
        assertThat(response.bodyString(), equalTo("body"))
    }

    @Test
    fun `performs simple GET request`() {
        val response = client(Request(GET, "http://httpbin.org/get").query("name", "John Doe"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("John Doe"))
    }

    @Test
    fun `performs simple POST request`() {
        val response = client(Request(POST, "http://httpbin.org/post"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(""))
    }

    @Test
    fun `performs simple DELETE request`() {
        val response = client(Request(DELETE, "http://httpbin.org/delete"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(""))
    }

    @Test
    fun `does not follow redirects`() {
        val response = client(Request(GET, "http://httpbin.org/redirect-to").query("url", "/destination"))

        assertThat(response.status, equalTo(Status.FOUND))
        assertThat(response.header("location"), equalTo("/destination"))
    }

    @Test
    fun `does not store cookies`() {
        client(Request(GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        val response = client(Request(GET, "http://httpbin.org/cookies"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), !containsSubstring("foo"))
    }

    @Test
    fun `filters enable cookies and redirects`() {
        val enhancedClient = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(client)

        val response = enhancedClient(Request(GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), containsSubstring("foo"))
    }

    @Test
    fun `empty body`() {
        val response = client(Request(Method.GET, "http://localhost:$port/empty"))
        response.status.successful.shouldMatch(equalTo(true))
        response.bodyString().shouldMatch(equalTo(""))
    }

    @Test
    fun `redirection response`() {
        val response = ClientFilters.FollowRedirects()
            .then(client)(Request(Method.GET, "http://httpbin.org/relative-redirect/5"))
        response.status.shouldMatch(equalTo(OK))
        response.bodyString().shouldMatch(anything)
    }

    @Test
    fun `send binary data`() {
        val response = client(Request(Method.POST, "http://localhost:$port/check-image").body(Body(ByteBuffer.wrap(testImageBytes()))))
        response.status.shouldMatch(equalTo(OK))
    }

    @Test
    open fun `socket timeouts are converted into 504`() {
        val response = timeoutClient(Request(GET, "http://localhost:$port/delay/150"))

        assertThat(response.status, equalTo(Status.CLIENT_TIMEOUT))
    }
}