package com.afonicos.pizarramangosusa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afonicos.pizarramangosusa.ui.theme.PizarraMangosUSATheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PizarraMangosUSATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Variables para controlar la navegación
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var userRole by remember { mutableStateOf("") }

                    if (!isLoggedIn) {
                        // 1. SI NO HA INICIADO SESIÓN: Mostramos tu pantalla de Login
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { rol ->
                                userRole = rol
                                isLoggedIn = true
                            }
                        )
                    } else {
                        // 2. SI YA INICIÓ SESIÓN: Mostramos el sistema principal

                        // Aquí ejecutamos la prueba que dejó tu compañero temporalmente
                        LaunchedEffect(Unit) {
                            val viewModel = com.afonicos.pizarramangosusa.model.MangosViewModel()
                            viewModel.registrarNuevaCompra("Productor de Prueba (Fuego)", 15.5, 25000.0)
                        }

                        // Pantalla temporal de bienvenida para comprobar que funcionó
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "¡Bienvenido a la Pizarra!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tu nivel de acceso es: ${userRole.uppercase()}",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                // Botón para cerrar sesión
                                FirebaseAuth.getInstance().signOut()
                                isLoggedIn = false
                            }) {
                                Text("Cerrar Sesión")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================================
// TU CÓDIGO: PANTALLA DE LOGIN
// ===================================================================
@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== AQUÍ AGREGAMOS EL LOGO ==========
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo_mangos), // <-- Asegúrate de que se llame igual que tu archivo
            contentDescription = "Logo Corporativo Mangos USA",
            modifier = Modifier
                .size(140.dp) // Ajusta aquí el tamaño que quieras para el logo
                .padding(bottom = 16.dp)
        )
        // ============================================

        Text(
            text = "Mangos U.S.A.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF003057)
        )
        Text(text = "Acceso Operativo", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = Color.Red, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Llena todos los campos."
                    return@Button
                }

                isLoading = true
                errorMessage = null

                auth.signInWithEmailAndPassword(email.trim(), password.trim())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                db.collection("usuarios").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        isLoading = false
                                        // Si no encuentra el rol, por defecto será capturista
                                        val rol = document.getString("rol") ?: "capturista"
                                        onLoginSuccess(rol)
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        errorMessage = "Error de red al verificar rol."
                                    }
                            }
                        } else {
                            isLoading = false
                            errorMessage = "Credenciales incorrectas."
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("INICIAR SESIÓN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}