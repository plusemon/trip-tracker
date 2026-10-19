package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.PdfStorageHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfStorageHelperTest {

    @Test
    fun testPdfStorageHelperAndResourceStrings() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Test string resources exist
        val saveText = context.getString(R.string.save_to_downloads)
        val shareText = context.getString(R.string.share_pdf)
        val openText = context.getString(R.string.open_pdf)

        assertTrue(saveText.isNotEmpty())
        assertTrue(shareText.isNotEmpty())
        assertTrue(openText.isNotEmpty())

        // Test PDF file creation and MediaStore saving logic
        val tempDir = File(context.cacheDir, "test_reports").apply { mkdirs() }
        val testFile = File(tempDir, "Test_Report.pdf").apply {
            writeText("%PDF-1.4 Mock Content")
        }
        assertTrue(testFile.exists())

        val uri = PdfStorageHelper.savePdfToPublicDownloads(context, testFile, "Test_Report.pdf")
        // On Robolectric with SDK 34, MediaStore insert returns a valid content URI
        assertNotNull(uri)
    }
}
