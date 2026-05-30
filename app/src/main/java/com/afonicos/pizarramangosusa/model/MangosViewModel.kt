package com.afonicos.pizarramangosusa.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.afonicos.pizarramangosusa.repository.MangosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MangosViewModel : ViewModel() {
    private val repository = MangosRepository()

    // 1. Estado de la Pizarra (La lista de transacciones que verá la pantalla)
    private val _compras = MutableStateFlow<List<CompraTransaccion>>(emptyList())
    val compras: StateFlow<List<CompraTransaccion>> = _compras.asStateFlow()

    // 2. Estado de los Totales (Contador de Progreso)
    private val _totalToneladas = MutableStateFlow(0.0)
    val totalToneladas: StateFlow<Double> = _totalToneladas.asStateFlow()

    private val _totalDinero = MutableStateFlow(0.0)
    val totalDinero: StateFlow<Double> = _totalDinero.asStateFlow()

    private var firebaseListener: ListenerRegistration? = null

    // Genera automáticamente la fecha de hoy con el formato exacto de nuestro documento
    private val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        // En cuanto el ViewModel nace, empieza a escuchar la base de datos
        iniciarSincronizacionTiempoReal()
    }

    private fun iniciarSincronizacionTiempoReal() {
        firebaseListener = repository.obtenerReferenciaCompras(fechaHoy)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val listaTemporal = mutableListOf<CompraTransaccion>()
                var sumaTons = 0.0
                var sumaDinero = 0.0

                // Firebase nos entrega los documentos nuevos y calculamos los totales
                for (documento in snapshot.documents) {
                    val compra = documento.toObject(CompraTransaccion::class.java)
                    if (compra != null) {
                        listaTemporal.add(compra)
                        sumaTons += compra.volumen_toneladas
                        sumaDinero += compra.monto_total
                    }
                }

                // Actualizamos los flujos (Jetpack Compose redibujará la pantalla automáticamente al ver este cambio)
                _compras.value = listaTemporal
                _totalToneladas.value = sumaTons
                _totalDinero.value = sumaDinero
            }
    }

    // Funciones que llamarán los botones de la Interfaz Gráfica
    fun registrarNuevaCompra(proveedor: String, toneladas: Double, monto: Double) {
        viewModelScope.launch {
            val nuevaCompra = CompraTransaccion(
                proveedor = proveedor,
                volumen_toneladas = toneladas,
                monto_total = monto
            )
            repository.registrarCompra(fechaHoy, nuevaCompra)
        }
    }

    fun eliminarCompra(idCompra: String) {
        viewModelScope.launch {
            repository.eliminarCompra(fechaHoy, idCompra)
        }
    }

    // Ahora la función está DENTRO de la clase y usa fechaHoy
    fun actualizarCompra(id: String, proveedor: String, toneladas: Double, monto: Double) {
        viewModelScope.launch {
            repository.actualizarCompra(fechaHoy, id, proveedor, toneladas, monto)
        }
    }

    // Evita fugas de memoria apagando el micrófono de Firebase si la app se cierra
    override fun onCleared() {
        super.onCleared()
        firebaseListener?.remove()
    }
}