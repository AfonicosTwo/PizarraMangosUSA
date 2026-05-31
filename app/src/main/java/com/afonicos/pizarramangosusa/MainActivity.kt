package com.afonicos.pizarramangosusa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.afonicos.pizarramangosusa.ui.PizarraScreen
import com.afonicos.pizarramangosusa.ui.theme.PizarraMangosUSATheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = com.afonicos.pizarramangosusa.model.MangosViewModel()

        setContent {
            PizarraMangosUSATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val auth = FirebaseAuth.getInstance()
                    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                    var userRole by remember { mutableStateOf("") }

                    // Nueva variable para mostrar pantalla de carga mientras buscamos el rol
                    var isCheckingRole by remember { mutableStateOf(auth.currentUser != null) }

                    LaunchedEffect(auth.currentUser) {
                        if (auth.currentUser != null) {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("usuarios").document(auth.currentUser!!.uid).get()
                                .addOnSuccessListener { document ->
                                    userRole = document.getString("rol") ?: "capturista"
                                    isCheckingRole = false // Terminó de cargar, ya puede mostrar la Pizarra
                                }
                                .addOnFailureListener {
                                    userRole = "capturista" // Seguridad por defecto si falla el internet
                                    isCheckingRole = false
                                }
                        } else {
                            isCheckingRole = false
                        }
                    }

                    if (isCheckingRole) {
                        // Pantalla de carga suave mientras averigua los permisos en Firebase
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2E7D32))
                        }
                    } else if (!isLoggedIn) {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLoginSuccess = { rol ->
                                userRole = rol
                                isLoggedIn = true
                            }
                        )
                    } else {
                        PizarraScreen(
                            viewModel = viewModel,
                            userRole = userRole,
                            onProfileClick = {
                                auth.signOut()
                                isLoggedIn = false
                                userRole = "" // Limpiamos el rol por seguridad al salir
                            }
                        )
                    }
                }
            }
        }
    }
}

// ===================================================================
// PANTALLA DE LOGIN
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo_mangos),
            contentDescription = "Logo Corporativo Mangos USA",
            modifier = Modifier
                .size(140.dp)
                .padding(bottom = 16.dp)
        )

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

        Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.Center) {
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Color.Red, fontWeight = FontWeight.Bold)
            }
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
                                db.collection("usuarios").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            // El usuario ya existía en la base de datos, entra normal
                                            isLoading = false
                                            val rol = document.getString("rol") ?: "capturista"
                                            onLoginSuccess(rol)
                                        } else {
                                            // AUTO-REGISTRO: Es su primera vez iniciando sesión
                                            val datosNuevoUsuario = hashMapOf(
                                                "correo" to email.trim(),
                                                "rol" to "capturista"
                                            )
                                            db.collection("usuarios").document(userId).set(datosNuevoUsuario)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    onLoginSuccess("capturista") // Entra como capturista por defecto
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                    errorMessage = "Error al registrar en base de datos."
                                                }
                                        }
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