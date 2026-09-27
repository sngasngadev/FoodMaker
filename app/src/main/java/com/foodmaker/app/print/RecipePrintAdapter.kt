package com.foodmaker.app.print

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
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
        paint.color = Color.rgb(45,45,45)
        paint.textSize = w * 0.045f
        paint.isFakeBoldText = true
        canvas.drawText("${recipe.title} - 오려서 놀기", w * 0.06f, h * 0.07f, paint)
        paint.isFakeBoldText = false
        paint.textSize = w * 0.022f
        canvas.drawText("점선을 따라 오린 뒤 순서대로 놓아보세요.", w * 0.06f, h * 0.105f, paint)

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

            val partRect = RectF(
                cell.left + cell.width() * 0.14f,
                cell.top + cell.height() * 0.09f,
                cell.right - cell.width() * 0.14f,
                cell.bottom - cell.height() * 0.25f
            )

            FoodPainter.drawPart(canvas, part, partRect, context.assets)
            drawCutGuide(canvas, part, partRect)

            paint.color = Color.DKGRAY
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = w * 0.024f
            canvas.drawText(part.label, cell.centerX(), cell.bottom - cell.height() * 0.07f, paint)
        }
    }

    private fun drawCutGuide(canvas: android.graphics.Canvas, part: PartDefinition, rect: RectF) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.7f
            pathEffect = DashPathEffect(floatArrayOf(7f,6f),0f)
            color = Color.rgb(150,150,150)
        }
        val pad = rect.width() * 0.045f
        val r = RectF(rect.left-pad,rect.top-pad,rect.right+pad,rect.bottom+pad)

        when (part.shape) {
            "circle","oval","rice","salmon" -> canvas.drawOval(r,p)
            "triangle" -> {
                val path = Path().apply {
                    moveTo(r.centerX(),r.top)
                    lineTo(r.right,r.bottom)
                    lineTo(r.left,r.bottom)
                    close()
                }
                canvas.drawPath(path,p)
            }
            "cheese" -> {
                val path = Path().apply {
                    moveTo(r.centerX(),r.top)
                    lineTo(r.right,r.centerY())
                    lineTo(r.centerX(),r.bottom)
                    lineTo(r.left,r.centerY())
                    close()
                }
                canvas.drawPath(path,p)
            }
            else -> canvas.drawRoundRect(r,20f,20f,p)
        }
    }
}
