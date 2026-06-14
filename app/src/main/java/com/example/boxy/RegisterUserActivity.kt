package com.example.boxy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackToLoginClick = { finish() },
                        onRegistrationSuccess = {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onBackToLoginClick: () -> Unit = {},
    onRegistrationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current

    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val database = remember { FirebaseDatabase.getInstance() }

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var codigoCaixa by remember { mutableStateOf("") }

    var senhaVisivel by remember { mutableStateOf(false) }
    var confirmarSenhaVisivel by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }

    val azulBotaoReal = Color(0xFF3370A6)
    val azulTextoTitulo = Color(0xFF0F3B66)
    val cinzaBordaSutil = Color(0xFFDCDFE4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_sdb),
                contentDescription = "Logo",
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Criar conta",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = azulTextoTitulo,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Configure sua Smart Delivery Box",
            fontSize = 13.sp,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {

            // --- NOME ---
            Text("Nome completo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("João Silva", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- E-MAIL ---
            Text("E-mail", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("seuemail@com.com", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- SENHA ---
            Text("Senha", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("••••••••", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }, enabled = !carregando) {
                        Icon(
                            imageVector = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = TextoSecundario.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONFIRMAR SENHA ---
            Text("Confirmar senha", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = confirmarSenha,
                onValueChange = { confirmarSenha = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("••••••••", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }, enabled = !carregando) {
                        Icon(
                            imageVector = if (confirmarSenhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = TextoSecundario.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CÓDIGO DA CAIXA ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Código da caixa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = "Ajuda",
                    tint = TextoSecundario,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(enabled = !carregando) {
                            Toast.makeText(context, "Insira o código impresso atrás ou no manual da sua caixa física.", Toast.LENGTH_LONG).show()
                        }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = codigoCaixa,
                onValueChange = { codigoCaixa = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("SDB-XXXXXX", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )
        } // CORRIGIDO: Agora fechamos a coluna do formulário corretamente aqui

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (nome.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank() || codigoCaixa.isBlank()) {
                    Toast.makeText(context, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                } else if (senha != confirmarSenha) {
                    Toast.makeText(context, "As senhas não coincidem.", Toast.LENGTH_SHORT).show()
                } else if (senha.length < 6) {
                    Toast.makeText(context, "A senha deve conter no mínimo 6 caracteres.", Toast.LENGTH_SHORT).show()
                } else {
                    carregando = true
                    firebaseAuth.createUserWithEmailAndPassword(email.trim(), senha)
                        .addOnCompleteListener { tarefaAuth ->
                            if (tarefaAuth.isSuccessful) {
                                val userId = firebaseAuth.currentUser?.uid

                                val dadosUsuario = mapOf(
                                    "nome" to nome.trim(),
                                    "email" to email.trim(),
                                    "boxCode" to codigoCaixa.trim().uppercase()
                                )

                                if (userId != null) {
                                    database.reference.child("users").child(userId)
                                        .setValue(dadosUsuario)
                                        .addOnSuccessListener {
                                            carregando = false
                                            Toast.makeText(context, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                                            onRegistrationSuccess()
                                        }
                                        .addOnFailureListener { erroDb ->
                                            carregando = false
                                            Toast.makeText(context, "Erro ao salvar dados: ${erroDb.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            } else {
                                carregando = false
                                Toast.makeText(context, "Erro no cadastro: ${tarefaAuth.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !carregando,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = azulBotaoReal)
        ) {
            if (carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Criar conta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val textoRodape = buildAnnotatedString {
            withStyle(style = SpanStyle(color = TextoSecundario, fontSize = 13.sp)) {
                append("Já tem uma conta? ")
            }
            withStyle(style = SpanStyle(color = azulBotaoReal, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                append("Fazer login")
            }
        }

        Text(
            text = textoRodape,
            modifier = Modifier
                .clickable(enabled = !carregando) { onBackToLoginClick() }
                .padding(bottom = 16.dp)
        )
    }
}

// CORRIGIDO: O Preview agora está isolado fora de qualquer outra classe no nível superior do arquivo
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    BoxyTheme {
        RegisterScreen(
            onBackToLoginClick = {},
            onRegistrationSuccess = {}
        )
    }
}