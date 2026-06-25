package com.tatamotors.hcvcalculator.pdf

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.tatamotors.hcvcalculator.R
import java.io.File
import java.io.FileOutputStream

/**
 * Branded A4 PDF estimates, drawn with the native PdfDocument API (fully offline).
 * Visual cues follow the Tata Motors CV creatives:
 *  - Header: official "TATA MOTORS Commercial Vehicles | Better Always" lockup
 *  - Tata blue (#00529C) headline band with campaign tagline
 *  - Clean tables, green hero band for the money figure
 *  - Dark footer: "TATA TRUCKS | DESH KE TRUCKS" + disclaimer
 */
object PdfShare {

    private const val PAGE_W = 595   // A4 @72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    val TATA_BLUE = Color.rgb(0, 82, 156)
    private val TATA_BLUE_DARK = Color.rgb(0, 58, 111)
    private val GREEN = Color.rgb(27, 135, 59)
    private val GREEN_LIGHT = Color.rgb(230, 244, 234)
    private val GREY = Color.rgb(96, 102, 110)
    private val ROW_ALT = Color.rgb(243, 247, 251)
    private val DARK = Color.rgb(24, 28, 33)

    class Pdf(private val context: Context) {
        private val doc = PdfDocument()
        private lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f
        private var pageNo = 0

        fun startPage() {
            pageNo += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
            drawHeader()
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_H - 90f) {
                drawFooter()
                doc.finishPage(page)
                startPage()
            }
        }

        private fun drawHeader() {
            try {
                val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.tata_lockup)
                val w = 230f
                val h = w * bmp.height / bmp.width
                canvas.drawBitmap(bmp, null, RectF(MARGIN, y, MARGIN + w, y + h), null)
                y += h + 10f
            } catch (_: Exception) {
                val p = paint(14f, TATA_BLUE_DARK, bold = true)
                canvas.drawText("TATA MOTORS  Commercial Vehicles | Better Always", MARGIN, y + 14f, p)
                y += 26f
            }
            val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
            y += 14f
        }

        fun drawFooter() {
            val bandTop = PAGE_H - 64f
            val band = Paint().apply { color = DARK }
            canvas.drawRect(0f, bandTop, PAGE_W.toFloat(), PAGE_H.toFloat(), band)
            val t1 = paint(12f, Color.rgb(74, 174, 233), bold = true)
            val t2 = paint(14f, Color.WHITE, bold = true)
            canvas.drawText("TATA TRUCKS", MARGIN, bandTop + 24f, t1)
            canvas.drawText("DESH KE TRUCKS", MARGIN, bandTop + 42f, t2)
            val disc = paint(7f, Color.rgb(180, 186, 193))
            canvas.drawText(
                "Indicative planning estimate. Actual results depend on route, load, driver habits & maintenance. T&C apply.",
                MARGIN, PAGE_H - 10f, disc
            )
        }

        /** Full-width truck/creative banner, aspect ratio preserved, capped height. */
        fun bannerImage(resId: Int, maxHeight: Float = 170f) {
            try {
                val bmp = BitmapFactory.decodeResource(context.resources, resId) ?: return
                val w = PAGE_W - 2 * MARGIN
                var h = w * bmp.height / bmp.width
                var drawW = w
                if (h > maxHeight) {           // too tall: keep height cap, center horizontally
                    drawW = maxHeight * bmp.width / bmp.height
                    h = maxHeight
                }
                ensureSpace(h + 12f)
                val left = MARGIN + (w - drawW) / 2f
                canvas.drawBitmap(bmp, null, RectF(left, y, left + drawW, y + h), null)
                y += h + 12f
            } catch (_: Exception) { /* banner is decorative; never block the estimate */ }
        }

        fun taglineBand(tagline: String, sub: String) {            ensureSpace(70f)
            val band = Paint().apply { color = TATA_BLUE }
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + 56f), 8f, 8f, band)
            canvas.drawText(tagline, MARGIN + 14f, y + 24f, paint(16f, Color.WHITE, bold = true))
            canvas.drawText(sub, MARGIN + 14f, y + 43f, paint(9.5f, Color.rgb(211, 230, 248)))
            y += 70f
        }

        fun sectionTitle(text: String) {
            ensureSpace(26f)
            canvas.drawText(text.uppercase(), MARGIN, y + 12f, paint(11f, TATA_BLUE_DARK, bold = true))
            y += 22f
        }

        fun kvBlock(pairs: List<Pair<String, String>>) {
            val rowH = 16f
            ensureSpace(pairs.size * rowH + 8f)
            pairs.forEach { (k, v) ->
                canvas.drawText(k, MARGIN, y + 11f, paint(9.5f, GREY))
                canvas.drawText(v, MARGIN + 170f, y + 11f, paint(9.5f, DARK, bold = true))
                y += rowH
            }
            y += 8f
        }

        /**
         * Simple table. colWeights sum to 1. boldRows are highlighted.
         * greenCol (per row): index of cell to tint green (better value), -1 = none.
         */
        fun table(
            headers: List<String>,
            rows: List<List<String>>,
            colWeights: List<Float>,
            boldRows: Set<Int> = emptySet(),
            greenCellPerRow: Map<Int, Int> = emptyMap()
        ) {
            val tableW = PAGE_W - 2 * MARGIN
            val rowH = 18f
            val xs = ArrayList<Float>()
            var acc = MARGIN
            colWeights.forEach { w -> xs.add(acc); acc += tableW * w }

            // header
            ensureSpace(rowH * 2)
            val head = Paint().apply { color = TATA_BLUE }
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + rowH, head)
            headers.forEachIndexed { i, h ->
                canvas.drawText(clip(h, colWeights[i]), xs[i] + 5f, y + 12.5f, paint(8.5f, Color.WHITE, bold = true))
            }
            y += rowH

            rows.forEachIndexed { r, row ->
                ensureSpace(rowH)
                if (r % 2 == 1) {
                    canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + rowH, Paint().apply { color = ROW_ALT })
                }
                row.forEachIndexed { i, cell ->
                    val green = greenCellPerRow[r] == i
                    val p = paint(
                        8.5f,
                        if (green) GREEN else DARK,
                        bold = boldRows.contains(r) || green
                    )
                    canvas.drawText(clip(cell, colWeights[i]), xs[i] + 5f, y + 12.5f, p)
                }
                y += rowH
            }
            y += 10f
        }

        fun heroBand(caption: String, amount: String, sub: String) {
            ensureSpace(80f)
            val band = Paint().apply { color = GREEN_LIGHT }
            canvas.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + 68f), 10f, 10f, band)
            canvas.drawText(caption, MARGIN + 14f, y + 20f, paint(10f, GREY, bold = true))
            canvas.drawText(amount, MARGIN + 14f, y + 46f, paint(22f, GREEN, bold = true))
            canvas.drawText(sub, MARGIN + 14f, y + 61f, paint(9f, DARK))
            y += 80f
        }

        fun note(text: String) {
            ensureSpace(16f)
            canvas.drawText(text, MARGIN, y + 10f, paint(8f, GREY))
            y += 16f
        }

        private fun clip(s: String, weight: Float): String {
            val maxChars = ((PAGE_W - 2 * MARGIN) * weight / 4.6f).toInt()
            return if (s.length <= maxChars) s else s.take(maxChars - 1) + "\u2026"
        }

        /** Finish the document and write it to cache, returning the file (no share yet). */
        fun finishToFile(fileName: String): File {
            drawFooter()
            doc.finishPage(page)
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            return file
        }

        /** Convenience: finish + immediately open the share sheet as PDF. */
        fun finishAndShare(fileName: String, shareTitle: String) {
            val file = finishToFile(fileName)
            sharePdf(context, file, shareTitle)
        }
    }

    fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Share an existing PDF file via the system share sheet. */
    fun sharePdf(context: Context, file: File, shareTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share estimate PDF"))
    }

    /** Item 23: render the PDF's first page to a JPG and share it as an image. */
    fun shareAsImage(context: Context, pdfFile: File, imageName: String, shareTitle: String) {
        try {
            val pfd = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val scale = 2
            val bmp = android.graphics.Bitmap.createBitmap(page.width * scale, page.height * scale, android.graphics.Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); pfd.close()
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val img = File(dir, imageName)
            FileOutputStream(img).use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uriFor(context, img))
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share estimate image"))
        } catch (e: Exception) {
            // fall back to PDF share if rendering fails
            sharePdf(context, pdfFile, shareTitle)
        }
    }

    /** Item 24: render the PDF's first page to a Bitmap for in-app preview. */
    fun renderFirstPage(pdfFile: File): android.graphics.Bitmap? = try {
        val pfd = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = android.graphics.pdf.PdfRenderer(pfd)
        val page = renderer.openPage(0)
        val scale = 2
        val bmp = android.graphics.Bitmap.createBitmap(page.width * scale, page.height * scale, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close(); renderer.close(); pfd.close()
        bmp
    } catch (e: Exception) { null }

    fun paint(size: Float, color: Int, bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }
}
