package com.afonicos.pizarramangosusa.model

import com.google.firebase.firestore.DocumentId

// Estructura para la Colección Principal: jornadas_operativas
data class JornadaOperativa(
    @DocumentId val id: String = "", // Guardaremos la fecha
    val meta_toneladas: Double = 0.0,
    val total_acumulado_toneladas: Double = 0.0,
    val total_acumulado_dinero: Double = 0.0,
    val estado: String = "abierta"
)

data class CompraTransaccion(
    @DocumentId val id: String = "", // Firebase generará este código automáticamente
    val proveedor: String = "",
    val volumen_toneladas: Double = 0.0,
    val monto_total: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(), // Para ordenar la pizarra cronológicamente
    val capturista_correo: String = ""
)