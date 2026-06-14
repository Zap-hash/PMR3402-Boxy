package com.example.boxy

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val database = remember { FirebaseDatabase.getInstance() }

    var nomeUsuario by remember { mutableStateOf("Carregando...") }
    var emailUsuario by remember { mutableStateOf("") }
    var notificacoesAtivas by remember { mutableStateOf(true) } // Estado real do Switch decorativo

    val azulHeader = Color(0xFF163E66)
    val azulTextoTitulo = Color(0xFF0F3B66)

    // Busca os dados do usuário atual em tempo real
    LaunchedEffect(Unit) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            emailUsuario = firebaseAuth.currentUser?.email ?: ""
            database.reference.child("users").child(uid)
                .get().addOnSuccessListener { snapshot ->
                    nomeUsuario = snapshot.child("nome").value?.toString() ?: "Usuário"
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER AZUL (PERFIL) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(azulHeader)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Configurações",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card de Perfil do Usuário
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone de avatar genérico
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = nomeUsuario,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = emailUsuario,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- CORPO DAS CONFIGURAÇÕES ---
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- GRUPO 1: CONFIGURAÇÕES DE SEGURANÇA E CAIXA ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    ConfigRowItem(
                        title = "Alterar senha do aplicativo",
                        subtitle = "Modificar sua senha de acesso",
                        icon = Icons.Filled.VpnKey,
                        iconTint = Color(0xFF3370A6),
                        iconBg = Color(0xFF3370A6).copy(alpha = 0.1f),
                        onClick = { /* Decorativo por enquanto */ }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    ConfigRowItem(
                        title = "Alterar senha da caixa",
                        subtitle = "Configurar pelo teclado físico",
                        icon = Icons.Filled.Lock,
                        iconTint = Color(0xFFF2A93B),
                        iconBg = Color(0xFFF2A93B).copy(alpha = 0.1f),
                        onClick = { /* Decorativo por enquanto */ }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    ConfigRowItem(
                        title = "Informações da caixa",
                        subtitle = "Código: 123456\n● Desconectada",
                        icon = Icons.Filled.Info,
                        iconTint = Color(0xFF8888FF),
                        iconBg = Color(0xFF8888FF).copy(alpha = 0.1f),
                        onClick = { /* Decorativo por enquanto */ },
                        isStatusItem = true // Trata a cor vermelha do "Desconectada"
                    )
                }
            }

            // --- GRUPO 2: NOTIFICAÇÕES (COM SWITCH ATIVO) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3370A6).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFF3370A6), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Notificações", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
                            Text(text = "Alertas de novas entregas", fontSize = 12.sp, color = TextoSecundario)
                        }
                    }
                    Switch(
                        checked = notificacoesAtivas,
                        onCheckedChange = { notificacoesAtivas = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2ECC71), // Verde correspondente ao layout
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- BOTÃO SAIR (FUNCIONAL) ---
            OutlinedButton(
                onClick = {
                    firebaseAuth.signOut()
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE54B4B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE54B4B).copy(alpha = 0.5f))
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// COMPONENTE AUXILIAR PARA CADA LINHA DA LISTA
@Composable
fun ConfigRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isStatusItem: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66))

                // Trata a estilização caso o texto possua a bolinha vermelha de "Desconectada"
                if (isStatusItem) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Código: 123456", fontSize = 12.sp, color = TextoSecundario)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Desconectada", fontSize = 12.sp, color = TextoSecundario)
                    }
                } else {
                    Text(text = subtitle, fontSize = 12.sp, color = TextoSecundario)
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}