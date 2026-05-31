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

    private val _compras = MutableStateFlow<List<CompraTransaccion>>(emptyList())
    val compras: StateFlow<List<CompraTransaccion>> = _compras.asStateFlow()

    private val _totalToneladas = MutableStateFlow(0.0)
    val totalToneladas: StateFlow<Double> = _totalToneladas.asStateFlow()

    private val _totalDinero = MutableStateFlow(0.0)
    val totalDinero: StateFlow<Double> = _totalDinero.asStateFlow()

    private val _metaToneladas = MutableStateFlow(0.0)
    val metaToneladas: StateFlow<Double> = _metaToneladas.asStateFlow()

    private var firebaseListener: ListenerRegistration? = null
    private var firebaseJornadaListener: ListenerRegistration? = null

    private val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        iniciarSincronizacionTiempoReal()
    }

    private fun iniciarSincronizacionTiempoReal() {
        // 1. Escuchar la lista de compras
        firebaseListener = repository.obtenerReferenciaCompras(fechaHoy)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val listaTemporal = mutableListOf<CompraTransaccion>()
                var sumaTons = 0.0
                var sumaDinero = 0.0

                for (documento in snapshot.documents) {
                    val compra = documento.toObject(CompraTransaccion::class.java)
                    if (compra != null) {
                        listaTemporal.add(compra)
                        sumaTons += compra.volumen_toneladas
                        sumaDinero += compra.monto_total
                    }
                }
                _compras.value = listaTemporal
                _totalToneladas.value = sumaTons
                _totalDinero.value = sumaDinero
            }

        // 2. Escuchar la meta diaria
        firebaseJornadaListener = repository.obtenerReferenciaJornada(fechaHoy)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    _metaToneladas.value = snapshot.getDouble("meta_toneladas") ?: 0.0
                }
            }
    }

    fun registrarNuevaCompra(proveedor: String, toneladas: Double, monto: Double, correo: String) {
        viewModelScope.launch {
            val nuevaCompra = CompraTransaccion(
                proveedor = proveedor,
                volumen_toneladas = toneladas,
                monto_total = monto,
                capturista_correo = correo
            )
            repository.registrarCompra(fechaHoy, nuevaCompra)
        }
    }

    fun eliminarCompra(idCompra: String) {
        viewModelScope.launch {
            repository.eliminarCompra(fechaHoy, idCompra)
        }
    }

    fun actualizarCompra(id: String, proveedor: String, toneladas: Double, monto: Double) {
        viewModelScope.launch {
            repository.actualizarCompra(fechaHoy, id, proveedor, toneladas, monto)
        }
    }

    // --- NUEVA FUNCIÓN: Guardar la meta ---
    fun guardarMetaDiaria(meta: Double) {
        viewModelScope.launch {
            repository.establecerMetaDiaria(fechaHoy, meta)
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseListener?.remove()
        firebaseJornadaListener?.remove()
    }
}