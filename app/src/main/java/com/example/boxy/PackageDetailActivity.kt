package com.example.boxy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackageDetailActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageId = intent.getStringExtra("PACKAGE_ID") ?: ""

        setContent {
            BoxyTheme {
                var pacote by remember { mutableStateOf<PackageModel?>(null) }
                var carregando by remember { mutableStateOf(true) }

                // Listener em tempo real do pacote selecionado
                LaunchedEffect(packageId) {
                    if (packageId.isNotBlank()) {
                        FirebaseDatabase.getInstance().reference.child("packages").child(packageId)
                            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                                    pacote = snapshot.getValue(PackageModel::class.java)?.copy(id = snapshot.key ?: "")
                                    carregando = false
                                }
                                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                                    carregando = false
                                }
                            })
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(pacote?.descricao ?: "Detalhes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF0F3B66))
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                        )
                    }
                ) { innerPadding ->
                    if (carregando) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF3370A6))
                        }
                    } else if (pacote == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Encomenda não encontrada.")
                        }
                    } else {
                        PackageDetailScreen(
                            modifier = Modifier.padding(innerPadding),
                            pacote = pacote!!,
                            onExcluirClick = {
                                FirebaseDatabase.getInstance().reference.child("packages").child(packageId)
                                    .removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Encomenda excluída!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PackageDetailScreen(
    modifier: Modifier = Modifier,
    pacote: PackageModel,
    onExcluirClick: () -> Unit
) {
    val isEntregue = pacote.status.lowercase() == "entregue"

    // Função de conversão do Timestamp do Firebase (String) para data legível
    val formatarData = { timestampStr: String ->
        try {
            val milissegundos = timestampStr.toLong()
            val sdf = SimpleDateFormat("dd 'de'开放 MMMM 'de' yyyy 'às' HH:mm", Locale("pt", "BR"))
            sdf.format(Date(milissegundos))
        } catch (e: Exception) {
            timestampStr
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CARD PRINCIPAL DE DETALHES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCDFE4))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Status Tag
                Text(
                    text = if (isEntregue) "Entregue" else "Aguardando",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEntregue) Color(0xFF27AE60) else Color(0xFFD35400),
                    modifier = Modifier
                        .background(if (isEntregue) Color(0xFFE1F8EB) else Color(0xFFFEF3D6), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Descrição", fontSize = 12.sp, color = TextoSecundario)
                Text(text = pacote.descricao, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66))

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Compartimento", fontSize = 12.sp, color = TextoSecundario)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AllInbox, contentDescription = null, tint = Color(0xFF3370A6), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = pacote.compartimento.replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F3B66))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Data de cadastro", fontSize = 12.sp, color = TextoSecundario)
                Text(text = formatarData(pacote.dataCriacao), fontSize = 14.sp, color = Color(0xFF0F3B66))

                // Só exibe o campo data de entrega se tiver sido entregue de fato
                if (isEntregue && pacote.dataEntrega.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Data de entrega", fontSize = 12.sp, color = TextoSecundario)
                    Text(text = formatarData(pacote.dataEntrega), fontSize = 14.sp, color = Color(0xFF0F3B66))
                }
            }
        }

        // --- SEÇÃO DINÂMICA: FOTO DA ENTREGA (ESP32-CAM) ---
        if (isEntregue) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCDFE4))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Foto da entrega", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (pacote.fotoUrl.isNotBlank()) {
                        // Carrega e gerencia o cache da foto vinda do Firebase Storage de forma assíncrona
                        AsyncImage(
                            model = pacote.fotoUrl,
                            contentDescription = "Foto da Caixa de Entregas",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFFF0F2F5), RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Caso o ESP mude o status mas o upload da foto atrase por oscilação de Wi-Fi
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFF0F2F5), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Processando imagem da câmera...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTÃO EXCLUIR REGISTRO ---
        OutlinedButton(
            onClick = onExcluirClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Text("Excluir encomenda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}