package com.afonicos.pizarramangosusa.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.afonicos.pizarramangosusa.model.CompraTransaccion
import com.google.firebase.firestore.SetOptions

class MangosRepository {

    private val db = FirebaseFirestore.getInstance()

    private val jornadasCollection = db.collection("jornadas_operativas")

    suspend fun establecerMetaDiaria(fechaJornada: String, meta: Double) {
        val datos = mapOf("meta_toneladas" to meta)
        jornadasCollection.document(fechaJornada).set(datos, SetOptions.merge()).await()
    }

    fun obtenerReferenciaJornada(fechaJornada: String) = jornadasCollection.document(fechaJornada)

    suspend fun registrarCompra(fechaJornada: String, compra: CompraTransaccion) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .add(compra)
            .await()
    }

    fun obtenerReferenciaCompras(fechaJornada: String): Query {
        return jornadasCollection.document(fechaJornada)
            .collection("compras")
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    suspend fun actualizarCompra(fechaJornada: String, idCompra: String, mapActualizaciones: Map<String, Any>) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .document(idCompra)
            .update(mapActualizaciones)
            .await()
    }

    suspend fun eliminarCompra(fechaJornada: String, idCompra: String) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .document(idCompra)
            .delete()
            .await()
    }
    suspend fun actualizarCompra(
        fechaJornada: String,
        idCompra: String,
        proveedor: String,
        toneladas: Double,
        monto: Double
    ) {
        val camposActualizados = mapOf(
            "proveedor" to proveedor,
            "volumen_toneladas" to toneladas,
            "monto_total" to monto
        )

        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .document(idCompra)
            .update(camposActualizados)
            .await()
    }
}