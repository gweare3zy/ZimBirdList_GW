package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Zim Bird Checklist", appName)
  }

  @Test
  fun `verify fileprovider can resolve cache file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val reportsDir = java.io.File(context.cacheDir, "reports")
    reportsDir.mkdirs()
    val testFile = java.io.File(reportsDir, "test.pdf")
    testFile.writeText("test")
    val uri = androidx.core.content.FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      testFile.canonicalFile
    )
    org.junit.Assert.assertNotNull(uri)
  }

  @Test
  fun `verify bird name text wrapping`() {
    val paint = android.graphics.Paint()
    val longName = "Southern Double-collared Sunbird"
    val firstWordWidth = paint.measureText("Southern")
    val wrapped = com.example.pdf.TripPdfExporter.wrapText(longName, paint, firstWordWidth + 1f)
    org.junit.Assert.assertTrue("Should wrap long bird name into multiple lines", wrapped.size >= 2)
    org.junit.Assert.assertEquals("Southern", wrapped[0])
  }
}
