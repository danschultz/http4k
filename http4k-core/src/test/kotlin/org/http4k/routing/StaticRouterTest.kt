package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri.Companion.of
import org.http4k.core.then
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.junit.Ignore
import org.junit.Test

class StaticRouterTest {

    private val pkg = this.javaClass.`package`.name.replace('.', '/')

    @Test
    fun `looks up contents of existing root file`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/svc/mybob.xml")))
        assertThat(result.bodyString(), equalTo("<xml>content</xml>"))
        assertThat(result.header("Content-Type"), equalTo(APPLICATION_XML.value))
    }

    @Test
    fun `can register custom mime types`() {
        val handler = "/svc" by static(Classpath(), "myxml" to APPLICATION_XML)
        val result = handler(Request(GET, of("/svc/mybob.myxml")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("<myxml>content</myxml>"))
        assertThat(result.header("Content-Type"), equalTo(APPLICATION_XML.value))
    }

    @Test
    fun `defaults to index html if is no route`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/svc")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("hello from the root index.html"))
        assertThat(result.header("Content-Type"), equalTo(TEXT_HTML.value))
    }

    @Test
    fun `defaults to index html if is no route - non-root-context`() {
        val handler = "/svc" by static(Classpath("org"))
        val result = handler(Request(GET, of("/svc")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("hello from the io index.html"))
        assertThat(result.header("Content-Type"), equalTo(TEXT_HTML.value))
    }

    @Test
    fun `non existing index html if is no route`() {
        val handler = "/svc" by static(Classpath("org/http4k"))
        val result = handler(Request(GET, of("/svc")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `looks up contents of existing subdir file - non-root context`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/svc/$pkg/StaticRouter.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(result.header("Content-Type"), equalTo("application/javascript"))
    }

    @Test
    fun `looks up contents of existing subdir file`() {
        val handler = "/" by static()
        val result = handler(Request(GET, of("/$pkg/StaticRouter.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(result.header("Content-Type"), equalTo("application/javascript"))
    }

    @Test
    fun `can alter the root path`() {
        val handler = "/svc" by static(Classpath(pkg))
        val result = handler(Request(GET, of("/svc/StaticRouter.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(result.header("Content-Type"), equalTo("application/javascript"))
    }

    @Test
    fun `looks up non existent-file`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/svc/NotHere.xml")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `cannot serve a directory`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/svc/org")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `looks up non existent path`() {
        val handler = "/svc" by static()
        val result = handler(Request(GET, of("/bob/StaticRouter.js")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `can't subvert the path`() {
        val handler = "/svc" by static()
        assertThat(handler(Request(GET, of("/svc/../svc/Bob.xml"))).status, equalTo(NOT_FOUND))
        assertThat(handler(Request(GET, of("/svc/~/.bashrc"))).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `as a router when does not fine file`() {
        val handler = "/svc" by static()

        assertThat(handler.match(Request(GET, of("/svc/../svc/Bob.xml"))), absent())
    }

    @Test
    fun `as a router finds file`() {
        val handler = "/svc" by static()
        val req = Request(GET, of("/svc/mybob.xml"))
        assertThat(handler.match(req)?.invoke(req)?.status, equalTo(OK))
    }

    @Test
    @Ignore
    fun `can add filter to router`() {
        val changePathFilter = Filter {
            next ->
            { next(it.uri(it.uri.path("/svc/mybob.xml"))) }
        }
        val handler = "/svc" by changePathFilter.then(static())
        val req = Request(GET, of("/svc/notmybob.xml"))
        assertThat(handler(req).status, equalTo(OK))
    }

    @Test
    fun `can add filter to a RoutingHttpHandler`() {
        val changePathFilter = Filter {
            next ->
            { next(it.uri(it.uri.path("/svc/mybob.xml"))) }
        }
        val handler = changePathFilter.then("/svc" by static())
        val req = Request(GET, of("/svc/notmybob.xml"))
        assertThat(handler(req).status, equalTo(OK))
    }

}