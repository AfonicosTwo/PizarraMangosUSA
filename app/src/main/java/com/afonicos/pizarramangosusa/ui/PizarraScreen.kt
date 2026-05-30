package com.afonicos.pizarramangosusa.ui

// --- TODAS LAS IMPORTACIONES AGRUPADAS EN LA PARTE SUPERIOR ---
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afonicos.pizarramangosusa.R
import com.afonicos.pizarramangosusa.model.MangosViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizarraScreen(viewModel: MangosViewModel) {
    val listaCompras by viewModel.compras.collectAsState()
    val totalToneladas by viewModel.totalToneladas.collectAsState()
    val totalDinero by viewModel.totalDinero.collectAsState()

    var mostrarDialogo by remember { mutableStateOf(false) }
    var compraAEditar by remember { mutableStateOf<com.afonicos.pizarramangosusa.model.CompraTransaccion?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pizarra Transaccional") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                mostrarDialogo = true
            }) {
                Text("+", fontSize = 24.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Toneladas", style = MaterialTheme.typography.labelMedium)
                        Text("${totalToneladas} T", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Inversión del Día", style = MaterialTheme.typography.labelMedium)
                        Text("$${totalDinero}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (listaCompras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay compras registradas el día de hoy", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaCompras) { compra ->
                        TarjetaTransaccionSwipeable(
                            compra = compra,
                            alEditar = {
                                compraAEditar = compra
                            },
                            alEliminar = {
                                viewModel.eliminarCompra(compra.id)
                            }
                        )
                    }
                }
            }
        }

        if (mostrarDialogo) {
            FormularioCompraDialog(
                alCerrar = { mostrarDialogo = false },
                alGuardar = { proveedor, toneladas, monto ->
                    viewModel.registrarNuevaCompra(proveedor, toneladas, monto)
                }
            )
        }
        compraAEditar?.let { compra ->
            EdicionCompraDialog(
                compra = compra,
                alCerrar = { compraAEditar = null },
                alActualizar = { proveedor, toneladas, monto ->
                    viewModel.actualizarCompra(compra.id, proveedor, toneladas, monto)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioCompraDialog(
    alCerrar: () -> Unit,
    alGuardar: (String, Double, Double) -> Unit
) {
    var proveedor by remember { mutableStateOf("") }
    var toneladas by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Registrar Nueva Compra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    label = { Text("Nombre del Proveedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toneladas,
                    onValueChange = { toneladas = it },
                    label = { Text("Volumen en Toneladas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto Total Monetario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val toneladasDouble = toneladas.toDoubleOrNull() ?: 0.0
                    val montoDouble = monto.toDoubleOrNull() ?: 0.0

                    if (proveedor.isNotBlank() && toneladasDouble > 0 && montoDouble > 0) {
                        alGuardar(proveedor, toneladasDouble, montoDouble)
                        alCerrar()
                    }
                }
            ) {
                Text("Guardar Transacción")
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TarjetaTransaccionSwipeable(
    compra: com.afonicos.pizarramangosusa.model.CompraTransaccion,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe")

    val limiteDeslizamiento = -300f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    offsetX = 0f
                    alEditar()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            ) {
                Icon(painterResource(id = R.drawable.escritura), contentDescription = "Editar", modifier = Modifier.size(24.dp))
            }

            IconButton(
                onClick = {
                    offsetX = 0f
                    alEliminar()
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
            ) {
                Icon(painterResource(id = R.drawable.papelera), contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX < limiteDeslizamiento / 2) limiteDeslizamiento else 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val nuevoOffset = offsetX + dragAmount
                        offsetX = nuevoOffset.coerceIn(limiteDeslizamiento, 0f)
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = compra.proveedor, fontWeight = FontWeight.Bold)
                    Text(text = "$${compra.monto_total}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = "${compra.volumen_toneladas} T",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdicionCompraDialog(
    compra: com.afonicos.pizarramangosusa.model.CompraTransaccion,
    alCerrar: () -> Unit,
    alActualizar: (String, Double, Double) -> Unit
) {
    // Iniciamos los estados con los valores actuales de la compra
    var proveedor by remember { mutableStateOf(compra.proveedor) }
    var toneladas by remember { mutableStateOf(compra.volumen_toneladas.toString()) }
    var monto by remember { mutableStateOf(compra.monto_total.toString()) }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Editar Transacción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it },
                    label = { Text("Proveedor") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toneladas,
                    onValueChange = { toneladas = it },
                    label = { Text("Toneladas") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monto,
                    onValueChange = { monto = it },
                    label = { Text("Monto Total") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val ton = toneladas.toDoubleOrNull() ?: 0.0
                val mon = monto.toDoubleOrNull() ?: 0.0
                if (proveedor.isNotBlank() && ton > 0 && mon > 0) {
                    alActualizar(proveedor, ton, mon)
                    alCerrar()
                }
            }) { Text("Actualizar") }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) { Text("Cancelar") }
        }
    )
}