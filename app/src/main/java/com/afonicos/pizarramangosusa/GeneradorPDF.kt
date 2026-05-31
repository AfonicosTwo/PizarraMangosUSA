package com.afonicos.pizarramangosusa

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.afonicos.pizarramangosusa.model.CompraTransaccion
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generarReporteJornadaPDF(
    context: android.content.Context,
    listaCompras: List<com.afonicos.pizarramangosusa.model.CompraTransaccion>,
    totalToneladas: Double,
    totalDinero: Double,
    metaToneladas: Double,
    fechaJornada: String // <-- 1. NUEVO PARÁMETRO AQUÍ
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paintTitulo = Paint().apply { textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
    val paintNormal = Paint().apply { textSize = 14f }

    val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    val fechaDescarga = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

    var posicionY = 50f

    canvas.drawText("Reporte de Jornada - Mangos", 50f, posicionY, paintTitulo)

    posicionY += 50f

    canvas.drawText("Fecha de la Jornada: $fechaJornada", 50f, posicionY, paintNormal)

    posicionY += 30f

    canvas.drawText("Reporte generado el: $fechaDescarga", 50f, posicionY, paintNormal)

    posicionY += 40f

    canvas.drawText("Meta: $metaToneladas T | Total: $totalToneladas T", 50f, posicionY, paintNormal)

    posicionY += 40f

    for (compra in listaCompras) {
        // ... (Tu código del for se queda igual) {
        canvas.drawText("- ${compra.proveedor}: ${compra.volumen_toneladas} T ($${compra.monto_total})", 50f, posicionY, paintNormal)
        posicionY += 25f
        canvas.drawText("Capturado por: ${compra.capturista_correo}", 50f, posicionY, paintNormal)
        posicionY += 35f
    }

    pdfDocument.finishPage(page)

    try {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Reporte_${System.currentTimeMillis()}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}