package com.afonicos.pizarramangosusa.ui

import com.afonicos.pizarramangosusa.generarReporteJornadaPDF
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afonicos.pizarramangosusa.R
import com.afonicos.pizarramangosusa.model.MangosViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizarraScreen(viewModel: MangosViewModel, userRole: String, onProfileClick: () -> Unit) {
    val listaCompras by viewModel.compras.collectAsState()
    val totalToneladas by viewModel.totalToneladas.collectAsState()
    val totalDinero by viewModel.totalDinero.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val metaToneladas by viewModel.metaToneladas.collectAsState()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var compraAEditar by remember { mutableStateOf<com.afonicos.pizarramangosusa.model.CompraTransaccion?>(null) }
    var mostrarPerfil by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pizarra Transaccional") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { mostrarPerfil=true}) {
                        Icon(
                            painter = painterResource(id = R.drawable.usuario),
                            contentDescription = "Perfil de Usuario",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = {
                            val fechaHoy = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                            generarReporteJornadaPDF(
                                context = context,
                                listaCompras = listaCompras,
                                totalToneladas = totalToneladas,
                                totalDinero = totalDinero,
                                metaToneladas = metaToneladas,
                                fechaJornada = fechaHoy
                            )
                        },
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.participacion),
                            contentDescription = "Exportar reporte de hoy",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (userRole == "administrador") {
                        FloatingActionButton(
                            onClick = {
                                val calendario = java.util.Calendar.getInstance()
                                android.app.DatePickerDialog(
                                    context,
                                    { _, anio, mes, dia ->
                                        // Formateamos la fecha para que coincida con el ID de Firestore (Ej: 30-05-2026)
                                        // Sumamos 1 al mes porque en Java los meses empiezan en 0
                                        val fechaFormateada = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia)
                                        descargarYExportarHistorial(
                                            context = context,
                                            fechaId = fechaFormateada,
                                            metaActual = metaToneladas
                                        )
                                    },
                                    calendario.get(java.util.Calendar.YEAR),
                                    calendario.get(java.util.Calendar.MONTH),
                                    calendario.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_my_calendar), // Ícono nativo de calendario
                                contentDescription = "Exportar historial",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // CONTENEDOR DERECHO: AGREGAR TRANSACCIÓN
                FloatingActionButton(onClick = { mostrarDialogo = true }) {
                    Text("+", fontSize = 24.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val metaToneladas by viewModel.metaToneladas.collectAsState()
            var mostrarDialogoMeta by remember { mutableStateOf(false) }

            val progreso = if (metaToneladas > 0) (totalToneladas / metaToneladas).toFloat() else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                onClick = {
                    if (userRole == "administrador") {
                        mostrarDialogoMeta = true
                    }
                }            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total / Meta", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${totalToneladas} / ${if (metaToneladas > 0) metaToneladas else "?"} T",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Inversión del Día", style = MaterialTheme.typography.labelMedium)
                            Text("$$totalDinero", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progreso.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF2E7D32),
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                    )

                    if (progreso >= 1f && metaToneladas > 0) {
                        Text(
                            text = "¡Meta diaria alcanzada!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                        )
                    } else if (metaToneladas == 0.0) {
                        Text(
                            text = if (userRole == "administrador") "Toca aquí para establecer la meta del día" else "Meta del día",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            if (mostrarDialogoMeta) {
                var metaInput by remember { mutableStateOf(if (metaToneladas > 0) metaToneladas.toString() else "") }

                AlertDialog(
                    onDismissRequest = { mostrarDialogoMeta = false },
                    title = { Text("Establecer Meta Diaria") },
                    text = {
                        OutlinedTextField(
                            value = metaInput,
                            onValueChange = { metaInput = it },
                            label = { Text("Toneladas objetivo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            val nuevaMeta = metaInput.toDoubleOrNull() ?: 0.0
                            if (nuevaMeta > 0) {
                                viewModel.guardarMetaDiaria(nuevaMeta)
                                mostrarDialogoMeta = false
                            }
                        }) { Text("Guardar Meta") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarDialogoMeta = false }) { Text("Cancelar") }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (listaCompras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay compras registradas el día de hoy", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Espacio extra al final para que los FABs no tapen nada
                ) {
                    items(listaCompras) { compra ->
                        TarjetaTransaccionSwipeable(
                            compra = compra,
                            alEditar = {
                                compraAEditar = compra
                            },
                            esAdministrador = (userRole == "administrador"),
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
    if (mostrarPerfil) {
        PerfilDialog(
            userRole = userRole,
            alCerrar = { mostrarPerfil = false },
            alCerrarSesion = {
                mostrarPerfil = false
                onProfileClick() // Usaremos este parámetro para avisarle a MainActivity que cierre sesión
            }
        )
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
    esAdministrador: Boolean,
    alEditar: () -> Unit,
    alEliminar: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe")

    val limiteDeslizamiento = if (esAdministrador) -300f else 0f
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilDialog(
    userRole: String,
    alCerrar: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val correoUsuario = auth.currentUser?.email ?: "Usuario"

    // Nuevo estado para controlar la pantalla del gerente
    var mostrarGestionEmpleados by remember { mutableStateOf(false) }

    if (mostrarGestionEmpleados) {
        GestionEmpleadosDialog(alCerrar = { mostrarGestionEmpleados = false })
    } else {
        AlertDialog(
            onDismissRequest = alCerrar,
            modifier = Modifier.fillMaxWidth(),
            title = {
                Text("Perfil de Usuario", fontWeight = FontWeight.Bold)
            },
            text = {
                // LA MAGIA ESTÁ AQUÍ: Agregamos verticalScroll y rememberScrollState
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.persona),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF2E7D32)
                    )

                    Text(
                        text = "Rol actual: ${userRole.uppercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = correoUsuario,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // EL BOTÓN EXCLUSIVO PARA ADMINISTRADORES
                    if (userRole == "administrador") {
                        Button(
                            onClick = { mostrarGestionEmpleados = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003057)) // Azul oscuro corporativo
                        ) {
                            Text("Gestionar Empleados")
                        }
                    }

                    Button(
                        onClick = {
                            if (auth.currentUser?.email != null) {
                                auth.sendPasswordResetEmail(auth.currentUser!!.email!!)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            android.widget.Toast.makeText(context, "Se envió un enlace a tu correo", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Error al enviar el correo", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Enviar enlace para cambiar contraseña", textAlign= androidx.compose.ui.text.style.TextAlign.Center)
                    }

                    OutlinedButton(
                        onClick = alCerrarSesion,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = alCerrar) {
                    Text("Volver a la Pizarra")
                }
            }
        )
    }
}
data class EmpleadoInfo(
    val id: String,
    val correo: String,
    val rol: String
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionEmpleadosDialog(alCerrar: () -> Unit) {
    var listaEmpleados by remember { mutableStateOf<List<EmpleadoInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    // Descargar la lista de usuarios al abrir el diálogo
    LaunchedEffect(Unit) {
        db.collection("usuarios").get()
            .addOnSuccessListener { result ->
                val empleados = result.map { doc ->
                    EmpleadoInfo(
                        id = doc.id,
                        correo = doc.getString("correo") ?: "Sin correo",
                        rol = doc.getString("rol") ?: "capturista"
                    )
                }
                listaEmpleados = empleados
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Gestión de Personal", fontWeight = FontWeight.Bold) },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            } else if (listaEmpleados.isEmpty()) {
                Text("No hay empleados registrados.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listaEmpleados) { empleado ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = empleado.correo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = empleado.rol.uppercase(),
                                        color = if (empleado.rol == "administrador") MaterialTheme.colorScheme.primary else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }

                                // Botón para alternar el rol
                                IconButton(
                                    onClick = {
                                        val nuevoRol = if (empleado.rol == "administrador") "capturista" else "administrador"
                                        db.collection("usuarios").document(empleado.id)
                                            .update("rol", nuevoRol)
                                            .addOnSuccessListener {
                                                // Actualizar la lista local para que la UI reaccione
                                                listaEmpleados = listaEmpleados.map {
                                                    if (it.id == empleado.id) it.copy(rol = nuevoRol) else it
                                                }
                                            }
                                    },modifier=Modifier.background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.escritura), // Reutilizamos tu ícono de editar
                                        contentDescription = "Cambiar Rol",
                                        tint = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = alCerrar) {
                Text("Cerrar Panel")
            }
        }
    )
}
fun descargarYExportarHistorial(
    context: android.content.Context,
    fechaId: String,
    metaActual: Double // Pasamos esto por si el documento antiguo no tenía la meta guardada
) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    // 1. Buscamos el documento del día en jornadas_operativas
    db.collection("jornadas_operativas").document(fechaId).get()
        .addOnSuccessListener { documentoDia ->
            if (documentoDia.exists()) {
                // Recuperamos los totales de ese día
                val totalToneladas = documentoDia.getDouble("totalToneladas") ?: 0.0
                val totalDinero = documentoDia.getDouble("totalDinero") ?: 0.0
                val metaHistórica = documentoDia.getDouble("metaToneladas") ?: metaActual

                // 2. Buscamos las transacciones anidadas de ese día
                db.collection("jornadas_operativas").document(fechaId).collection("compras").get()
                    .addOnSuccessListener { snapshotCompras ->
                        val listaComprasHistoricas = snapshotCompras.toObjects(com.afonicos.pizarramangosusa.model.CompraTransaccion::class.java)

                        if (listaComprasHistoricas.isEmpty()) {
                            android.widget.Toast.makeText(context, "No hay compras registradas en esa fecha", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            // 3. Reutilizamos tu generador de PDF con los datos del pasado
                            com.afonicos.pizarramangosusa.generarReporteJornadaPDF(
                                context = context,
                                listaCompras = listaComprasHistoricas,
                                totalToneladas = totalToneladas,
                                totalDinero = totalDinero,
                                metaToneladas = metaHistórica,
                                fechaJornada = fechaId
                            )
                        }
                    }
            } else {
                android.widget.Toast.makeText(context, "No existe registro de jornada para el $fechaId", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener {
            android.widget.Toast.makeText(context, "Error de red al buscar historial", android.widget.Toast.LENGTH_SHORT).show()
        }
}