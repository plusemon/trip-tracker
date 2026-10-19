package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PayerType
import com.example.data.entity.TripEntity
import com.example.domain.model.CurrencyFormatter
import com.example.domain.model.MemberBalance
import com.example.domain.model.SettlementTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateReport(
        context: Context,
        trip: TripEntity,
        balances: List<MemberBalance>,
        settlements: List<SettlementTransaction>,
        expenses: List<ExpenseEntity>,
        totalDeposits: Double,
        commonFundSpent: Double,
        remainingCommonFund: Double
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var y = 40f

        // 1. Header Banner
        paint.color = Color.parseColor("#006C4C")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Trip Tracker — Tour Settlement Report", 30f, 42f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Group Travel Expense & Shared Wallet Manager", 30f, 58f, paint)

        y = 90f

        // 2. Trip Metadata
        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(trip.name, 30f, y, paint)

        y += 16f
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#49454F")
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val datesStr = "${dateFormat.format(Date(trip.startDate))} - ${dateFormat.format(Date(trip.endDate))}"
        canvas.drawText("Destination: ${trip.destination}   |   Dates: $datesStr   |   Tour Manager: ${trip.managerName}", 30f, y, paint)

        y += 24f

        // 3. Summary KPI Box
        paint.color = Color.parseColor("#F4FBF6")
        canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 60f, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#B7E1CD")
        canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 60f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        val totalSpentOverall = expenses.sumOf { it.amount }
        val colW = (pageWidth - 60) / 4f

        val kpis = listOf(
            "Group Fund Deposits" to CurrencyFormatter.formatTaka(totalDeposits, trip.currencySymbol),
            "Group Fund Spent" to CurrencyFormatter.formatTaka(commonFundSpent, trip.currencySymbol),
            "Group Fund Balance" to CurrencyFormatter.formatTaka(remainingCommonFund, trip.currencySymbol),
            "Total Expenses" to CurrencyFormatter.formatTaka(totalSpentOverall, trip.currencySymbol)
        )

        for (i in kpis.indices) {
            val (label, value) = kpis[i]
            val kpiX = 40f + i * colW
            paint.color = Color.parseColor("#49454F")
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(label, kpiX, y + 22f, paint)

            paint.color = if (i == 2 && remainingCommonFund < 0) Color.parseColor("#BA1A1A") else Color.parseColor("#006C4C")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(value, kpiX, y + 42f, paint)
        }

        y += 80f

        // 4. Member Ledgers Table
        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. Individual Member Ledgers", 30f, y, paint)

        y += 14f

        // Table Header
        paint.color = Color.parseColor("#E8F5E9")
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, paint)

        paint.color = Color.parseColor("#004D34")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val mCols = listOf(35f to "Member Name", 170f to "Deposited", 260f to "Pocket Paid", 350f to "Consumed", 440f to "Net Balance")
        for ((xPos, title) in mCols) {
            canvas.drawText(title, xPos, y + 14f, paint)
        }

        y += 20f
        paint.typeface = Typeface.DEFAULT

        for (mb in balances) {
            paint.color = Color.parseColor("#1C1B1F")
            canvas.drawText(mb.member.name, 35f, y + 14f, paint)
            canvas.drawText(CurrencyFormatter.formatTaka(mb.totalDeposited, trip.currencySymbol), 170f, y + 14f, paint)
            canvas.drawText(CurrencyFormatter.formatTaka(mb.totalPocketPaid, trip.currencySymbol), 260f, y + 14f, paint)
            canvas.drawText(CurrencyFormatter.formatTaka(mb.totalConsumed, trip.currencySymbol), 350f, y + 14f, paint)

            val netStr = when {
                mb.isCreditor -> "+${CurrencyFormatter.formatTaka(mb.netBalance, trip.currencySymbol)} (Refund)"
                mb.isDebtor -> "-${CurrencyFormatter.formatTaka(kotlin.math.abs(mb.netBalance), trip.currencySymbol)} (Owes)"
                else -> "Balanced"
            }
            paint.color = if (mb.isCreditor) Color.parseColor("#1B6D24") else if (mb.isDebtor) Color.parseColor("#BA1A1A") else Color.GRAY
            canvas.drawText(netStr, 440f, y + 14f, paint)

            // Divider line
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(30f, y + 18f, (pageWidth - 30).toFloat(), y + 18f, paint)
            y += 18f
        }

        y += 20f

        // 5. Minimized Settlement Instructions
        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. Step-by-Step Settlement Instructions", 30f, y, paint)

        y += 16f
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT

        if (settlements.isEmpty()) {
            paint.color = Color.parseColor("#1B6D24")
            canvas.drawText("✓ All debts are settled! No further cash transfers required.", 35f, y + 12f, paint)
            y += 20f
        } else {
            for ((idx, st) in settlements.withIndex()) {
                paint.color = Color.parseColor("#1C1B1F")
                val stepText = "${idx + 1}. ${st.fromMemberName}  ➡️  pays ${CurrencyFormatter.formatTaka(st.amount, trip.currencySymbol)}  to  ${st.toMemberName}"
                canvas.drawText(stepText, 35f, y + 12f, paint)
                y += 16f
            }
        }

        y += 20f

        // 6. Recent Itemized Expenses
        paint.color = Color.parseColor("#1C1B1F")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3. Itemized Tour Expenses (${expenses.size} Items)", 30f, y, paint)

        y += 14f

        // Expenses Table Header
        paint.color = Color.parseColor("#F0F4F8")
        canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 18f, paint)

        paint.color = Color.parseColor("#003355")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val expCols = listOf(35f to "Date", 110f to "Expense Title", 330f to "Category", 440f to "Payer", 500f to "Amount")
        for ((xPos, title) in expCols) {
            canvas.drawText(title, xPos, y + 13f, paint)
        }

        y += 18f
        paint.typeface = Typeface.DEFAULT

        val expDateFormat = SimpleDateFormat("dd/MM", Locale.US)
        for (e in expenses.take(15)) {
            if (y > pageHeight - 50) break // Fit comfortably
            paint.color = Color.parseColor("#1C1B1F")
            canvas.drawText(expDateFormat.format(Date(e.date)), 35f, y + 12f, paint)

            val shortTitle = if (e.title.length > 35) e.title.take(32) + "..." else e.title
            canvas.drawText(shortTitle, 110f, y + 12f, paint)
            canvas.drawText(e.category.displayName, 330f, y + 12f, paint)

            val payerText = if (e.payerType == PayerType.COMMON_FUND) "Group Fund" else "Member Pocket"
            canvas.drawText(payerText, 440f, y + 12f, paint)

            paint.color = Color.parseColor("#006C4C")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(CurrencyFormatter.formatTaka(e.amount, trip.currencySymbol), 500f, y + 12f, paint)
            paint.typeface = Typeface.DEFAULT

            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawLine(30f, y + 16f, (pageWidth - 30).toFloat(), y + 16f, paint)
            y += 16f
        }

        // Footer
        paint.color = Color.parseColor("#888888")
        paint.textSize = 8f
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("Generated on $generatedAt by Trip Tracker (Offline Mode)  |  Page 1 of 1", 30f, pageHeight - 20f, paint)

        pdfDocument.finishPage(page)

        // Save to cache reports directory
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeTripName = trip.name.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val pdfFile = File(reportsDir, "TripSplit_${safeTripName}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }
}
