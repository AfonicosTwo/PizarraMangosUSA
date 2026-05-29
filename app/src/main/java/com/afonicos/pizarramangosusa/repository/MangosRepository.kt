package com.afonicos.pizarramangosusa.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.afonicos.pizarramangosusa.model.CompraTransaccion

class MangosRepository {

    // 1. Inicializamos la conexión directa con tu base de datos en la nube
    private val db = FirebaseFirestore.getInstance()

    // 2. Guardamos la "ruta" principal para no escribirla a cada rato
    private val jornadasCollection = db.collection("jornadas_operativas")

    // --- OPERACIONES CRUD (Create, Read, Update, Delete) ---

    // CREATE: Inserta una nueva etiqueta en la pizarra del día
    suspend fun registrarCompra(fechaJornada: String, compra: CompraTransaccion) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .add(compra)
            .await() // Pausa la corrutina hasta que Google confirme que se guardó
    }

    // READ: Prepara la consulta para leer la pizarra en tiempo real (ordenada de la más nueva a la más vieja)
    fun obtenerReferenciaCompras(fechaJornada: String): Query {
        return jornadasCollection.document(fechaJornada)
            .collection("compras")
            .orderBy("timestamp", Query.Direction.DESCENDING)
    }

    // UPDATE: Modifica una transacción si el capturista se equivocó
    suspend fun actualizarCompra(fechaJornada: String, idCompra: String, mapActualizaciones: Map<String, Any>) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .document(idCompra)
            .update(mapActualizaciones)
            .await()
    }

    // DELETE: Borra una transacción permanentemente
    suspend fun eliminarCompra(fechaJornada: String, idCompra: String) {
        jornadasCollection.document(fechaJornada)
            .collection("compras")
            .document(idCompra)
            .delete()
            .await()
    }
}