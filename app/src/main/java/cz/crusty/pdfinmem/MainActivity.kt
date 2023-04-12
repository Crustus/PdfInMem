package cz.crusty.pdfinmem

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.system.Os
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var renderer: PdfRenderer
    private lateinit var pageBitmap: Bitmap
    private var currentPage = 0

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        getPdfFromUrl("https://www.adobe.com/support/products/enterprise/knowledgecenter/media/c4611_sample_explain.pdf") {testData ->
            val fd = Os.memfd_create("memFdTest", 0)
            Os.write(fd, testData, 0, testData.size)
            val readPipe = ParcelFileDescriptor.dup(fd)

            renderer = PdfRenderer(readPipe)

            showPrev()
        }

        pageBitmap = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)

        findViewById<ImageView>(R.id.page).apply {
            setImageBitmap(pageBitmap)
        }

        findViewById<Button>(R.id.left).setOnClickListener {
            showPrev()
        }

        findViewById<Button>(R.id.right).setOnClickListener {
            showNext()
        }
    }

    private fun getPdfFromUrl(address: String, onData: (ByteArray) -> Unit) {
        Thread {
            val outputStream = ByteArrayOutputStream()

            var totalBytesRead: Int = 0

            try {
                val chunk = ByteArray(4096)
                var bytesRead: Int

                val stream: InputStream = URL(address).openStream()
                while (stream.read(chunk).also { bytesRead = it } > 0) {
                    Timber.d(" - bytes read: $bytesRead")
                    outputStream.write(chunk, 0, bytesRead)
                    totalBytesRead += bytesRead
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return@Thread
            }

            val outData = outputStream.toByteArray()
            Timber.d("pdf byte size: ${outData.size}, totalBRead: $totalBytesRead")
            onData(outData)

        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.close()
        pageBitmap.recycle()
    }

    private fun showPrev() {
        currentPage--
        if (currentPage < 0) {
            currentPage = 0
        }
        showPage(currentPage)
    }

    private fun showNext() {
        currentPage++
        if (currentPage >= renderer.pageCount) {
            currentPage = renderer.pageCount - 1
        }
        showPage(currentPage)
    }

    private fun showPage(pageIndex: Int) {
        val page: PdfRenderer.Page = renderer.openPage(pageIndex)
        pageBitmap.eraseColor(Color.WHITE)
        page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
    }
}