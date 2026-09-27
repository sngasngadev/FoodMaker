package com.foodmaker.app.print

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.Recipe
import com.foodmaker.app.ui.FoodPainter
import java.io.FileOutputStream

class RecipePrintAdapter(
    private val context: Context,
    private val recipe: Recipe,
    private val parts: Map<String, PartDefinition>
) : PrintDocumentAdapter() {

    private var attributes: PrintAttributes? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        attributes = newAttributes
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("FoodMaker_${recipe.id}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val attrs = attributes ?: run {
            callback.onWriteFailed("Print attributes missing")
            return
        }

        val document = PrintedPdfDocument(context, attrs)
        try {
            val page = document.startPage(0)
            drawPage(page)
            document.finishPage(page)
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (t: Throwable) {
            callback.onWriteFailed(t.message)
        } finally {
            document.close()
        }
    }

    private fun drawPage(page: PdfDocument.Page) {
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = page.info.pageWidth.toFloat()
        val h = page.info.pageHeight.toFloat()

        canvas.drawColor(Color.WHITE)
        paint.color = Color.rgb(45, 45, 45)
        paint.textSize = w * 0.045f
        paint.isFakeBoldText = true
        canvas.drawText("${recipe.title} - 오려서 놀기", w * 0.06f, h * 0.07f, paint)
        paint.isFakeBoldText = false
        paint.textSize = w * 0.022f
        canvas.drawText("점선을 따라 오린 뒤 게임에서 만든 순서대로 놓아보세요.", w * 0.06f, h * 0.105f, paint)

        val ids = recipe.printParts
        val columns = 2
        val gap = w * 0.035f
        val left = w * 0.06f
        val top = h * 0.15f
        val cellW = (w - left * 2 - gap) / columns
        val rows = ((ids.size + columns - 1) / columns).coerceAtLeast(1)
        val cellH = ((h - top - h * 0.06f) / rows).coerceAtMost(h * 0.24f)

        ids.forEachIndexed { index, id ->
            val part = parts[id] ?: return@forEachIndexed
            val col = index % columns
            val row = index / columns
            val cell = RectF(
                left + col * (cellW + gap),
                top + row * cellH,
                left + col * (cellW + gap) + cellW,
                top + row * cellH + cellH - gap
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f, 7f), 0f)
            paint.color = Color.LTGRAY
            canvas.drawRoundRect(cell, 16f, 16f, paint)
            paint.pathEffect = null
            paint.style = Paint.Style.FILL

            val partRect = RectF(
                cell.left + cell.width() * 0.16f,
                cell.top + cell.height() * 0.12f,
                cell.right - cell.width() * 0.16f,
                cell.bottom - cell.height() * 0.24f
            )
            FoodPainter.drawPart(canvas, part, partRect)

            paint.color = Color.DKGRAY
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = w * 0.025f
            canvas.drawText(part.label, cell.centerX(), cell.bottom - cell.height() * 0.08f, paint)
        }
    }
}
