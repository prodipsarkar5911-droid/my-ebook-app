package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.HtmlSanitizerValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("eBook Processor", appName)
  }

  @Test
  fun `validate strict html 32 sanitizer`() {
    val input = "<section><main><h1>Chapter 1</h1><p>Body paragraph one.</p><p style=\"color: red;\">Paragraph two.</p></main></section>"
    val sanitized = HtmlSanitizerValidator.sanitizeToStrictHtml32(input)
    val validation = HtmlSanitizerValidator.validateHtml32(sanitized)

    assertTrue(validation.isStrictHtml32)
    assertEquals(2, validation.paragraphsWithIndentCount)
    assertEquals(1, validation.headingsCount["h1"])
  }
}

