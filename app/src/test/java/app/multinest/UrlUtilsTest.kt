package app.multinest

import app.multinest.data.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {
    @Test fun addsHttps() = assertEquals("https://instagram.com", UrlUtils.normalize("instagram.com"))
    @Test fun keepsScheme() = assertEquals("http://example.com/a", UrlUtils.normalize("http://example.com/a"))
    @Test fun rejectsEmpty() = assertNull(UrlUtils.normalize("  "))
    @Test fun rejectsSpaces() = assertNull(UrlUtils.normalize("hello world"))
    @Test fun rejectsBareWord() = assertNull(UrlUtils.normalize("hello"))
    @Test fun searchFallback() = assertTrue(UrlUtils.toAddress("hello world").startsWith("https://duckduckgo.com/?q=hello"))
}
