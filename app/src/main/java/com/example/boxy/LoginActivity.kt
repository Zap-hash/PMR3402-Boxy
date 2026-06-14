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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
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

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onRegisterClick = {
                            // Abre a tela de cadastro
                            val intent = Intent(this, RegisterUserActivity::class.java)
                            startActivity(intent)
                        },
                        onLoginSuccess = {
                            // Abre a MainActivity e limpa a pilha de telas para o usuário não voltar ao login com o botão "Voltar"
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
fun LoginScreen(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    // Estados locais para as entradas do usuário e controle de UI
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var carregando by remember { mutableStateOf(false) }

    // --- LOGICA DE PERSISTENCIA ---
    // Se o usuário já estiver logado anteriormente, pula direto para a tela principal
    LaunchedEffect(Unit) {
        if (firebaseAuth.currentUser != null) {
            onLoginSuccess()
        }
    }

    // Mantendo a paleta idêntica à tela de cadastro para consistência visual
    val azulBotaoReal = Color(0xFF3370A6)
    val azulTextoTitulo = Color(0xFF0F3B66)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Card flutuante com a imagem do seu Logotipo
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

        Spacer(modifier = Modifier.height(24.dp))

        // Títulos Principais da Tela
        Text(
            text = "Fazer login",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = azulTextoTitulo,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Gerencie suas entregas de forma segura",
            fontSize = 13.sp,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 36.dp)
        )

        // Campos do Formulário
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- CAMPO E-MAIL ---
            Text("E-mail", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("seuemail@com.com", color = TextoSecundario.copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- CAMPO SENHA ---
            Text("Senha", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                enabled = !carregando,
                singleLine = true,
                placeholder = { Text("••••••••", color = TextoSecundario.copy(alpha = 0.6f)) },
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
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Botão Principal: Entrar
        Button(
            onClick = {
                if (email.isBlank() || senha.isBlank()) {
                    Toast.makeText(context, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                } else {
                    carregando = true
                    firebaseAuth.signInWithEmailAndPassword(email.trim(), senha)
                        .addOnCompleteListener { tarefaLogin ->
                            if (tarefaLogin.isSuccessful) {
                                carregando = false
                                Toast.makeText(context, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                carregando = false
                                val erro = tarefaLogin.exception?.message ?: "Erro desconhecido"
                                Toast.makeText(context, "Falha ao entrar: $erro", Toast.LENGTH_LONG).show()
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
                Text("Entrar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Texto clicável de rodapé bicolor ("Cadastre-se" destacado)
        val textoRodape = buildAnnotatedString {
            withStyle(style = SpanStyle(color = TextoSecundario, fontSize = 13.sp)) {
                append("Não tem uma conta? ")
            }
            withStyle(style = SpanStyle(color = azulBotaoReal, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                append("Cadastre-se")
            }
        }

        Text(
            text = textoRodape,
            modifier = Modifier
                .clickable(enabled = !carregando) { onRegisterClick() }
                .padding(bottom = 16.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    BoxyTheme {
        LoginScreen()
    }
}