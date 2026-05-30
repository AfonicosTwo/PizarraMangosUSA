package com.afonicos.pizarramangosusa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afonicos.pizarramangosusa.model.MangosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizarraScreen(viewModel: MangosViewModel) {
    // Observadores de la base de datos en tiempo real
    val listaCompras by viewModel.compras.collectAsState()
    val totalToneladas by viewModel.totalToneladas.collectAsState()
    val totalDinero by viewModel.totalDinero.collectAsState()

    // Variable para controlar si el formulario emergente es visible
    var mostrarDialogo by remember { mutableStateOf(false) }

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
            // Tarjeta de Contadores (Total de Toneladas y Dinero)
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

            // Lista de Transacciones de la jornada
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
            }
        }

        // Formulario Emergente para capturar una nueva compra
        if (mostrarDialogo) {
            FormularioCompraDialog(
                alCerrar = { mostrarDialogo = false },
                alGuardar = { proveedor, toneladas, monto ->
                    viewModel.registrarNuevaCompra(proveedor, toneladas, monto)
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