package com.example.boxy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val database = remember { FirebaseDatabase.getInstance() }

    var nomeUsuario by remember { mutableStateOf("Usuário") }
    var caixaTravada by remember { mutableStateOf(true) }

    // CONTROLADOR DA ABA ATUAL: 0 = Início, 1 = Encomendas, 2 = Configurações
    var abaSelecionada by remember { mutableStateOf(0) }

    val azulIconeEncomenda = Color(0xFF3370A6)

    LaunchedEffect(Unit) {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            database.reference.child("users").child(uid).child("nome")
                .get().addOnSuccessListener { snapshot ->
                    snapshot.value?.toString()?.let { nomeCompleto ->
                        nomeUsuario = nomeCompleto.split(" ").firstOrNull() ?: nomeCompleto
                    }
                }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = abaSelecionada == 0,
                    onClick = { abaSelecionada = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Início") },
                    label = { Text("Início", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = azulIconeEncomenda,
                        selectedTextColor = azulIconeEncomenda,
                        indicatorColor = azulIconeEncomenda.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = abaSelecionada == 1,
                    onClick = { abaSelecionada = 1 },
                    icon = { Icon(Icons.Filled.AllInbox, contentDescription = "Encomendas") },
                    label = { Text("Encomendas", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = azulIconeEncomenda,
                        selectedTextColor = azulIconeEncomenda,
                        indicatorColor = azulIconeEncomenda.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = abaSelecionada == 2,
                    onClick = { abaSelecionada = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Configurações") },
                    label = { Text("Configurações", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = azulIconeEncomenda,
                        selectedTextColor = azulIconeEncomenda,
                        indicatorColor = azulIconeEncomenda.copy(alpha = 0.1f),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->

        // --- O MÁGICO DOS ESTADOS ---
        // Ele lê o valor de 'abaSelecionada' e renderiza apenas a tela correspondente
        when (abaSelecionada) {
            0 -> HomeScreenContent(
                modifier = Modifier.padding(innerPadding),
                nomeUsuario = nomeUsuario,
                caixaTravada = caixaTravada,
                onStatusCardClick = { caixaTravada = !caixaTravada }
            )
            1 -> {
                PackagesTabScreen(modifier = Modifier.padding(innerPadding))
            }
            2 -> {
                // Chama a tela externa que você criar (ex: SettingsScreen)
                SettingsScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

// ================= COMPONENTE DA TELA INICIAL (ISOLADO) =================
@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    nomeUsuario: String,
    caixaTravada: Boolean,
    onStatusCardClick: () -> Unit
) {

    val context = LocalContext.current

    val azulHeader = Color(0xFF163E66)
    val azulTextoTitulo = Color(0xFF0F3B66)
    val azulIconeEncomenda = Color(0xFF3370A6)
    val laranjaIconeHorario = Color(0xFFF2A93B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER AZUL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(azulHeader)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Bem-vindo", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        text = nomeUsuario.lowercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { /* Ação de notificações */ }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notificações",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CARD DE STATUS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable { onStatusCardClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (caixaTravada) Color(0xFFE54B4B) else Color(0xFF4CAF50),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (caixaTravada) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "Status Tranca",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Status da caixa", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(
                        text = if (caixaTravada) "Travada" else "Destravada",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- CONTEÚDO DO CORPO ---
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text(text = "Ações rápidas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                QuickActionCard(
                    title = "Nova encomenda",
                    icon = Icons.Filled.AllInbox,
                    iconContainerColor = azulIconeEncomenda.copy(alpha = 0.12f),
                    iconTint = azulIconeEncomenda,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Ação */
                        val intent = android.content.Intent(context, RegisterPackageActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                QuickActionCard(
                    title = "Configurar horário",
                    icon = Icons.Filled.AccessTime,
                    iconContainerColor = laranjaIconeHorario.copy(alpha = 0.12f),
                    iconTint = laranjaIconeHorario,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Ação */
                        val intent = android.content.Intent(context, RegisterTimeActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Últimas entregas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
                Text(
                    text = "Ver todas",
                    fontSize = 13.sp,
                    color = azulIconeEncomenda,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { /* Ver todas */ }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            DeliveryItemRow(title = "Medicamentos da Farmácia", date = "26 de abril às 00:23", imagePlaceholder = R.drawable.logo_sdb)
            Spacer(modifier = Modifier.height(12.dp))
            DeliveryItemRow(title = "Notebook Dell", date = "23 de abril às 01:23", imagePlaceholder = R.drawable.logo_sdb)
        }
    }
}

// Os componentes QuickActionCard e DeliveryItemRow permanecem iguaizinhos no final do arquivo...
@Composable
fun QuickActionCard(title: String, icon: ImageVector, iconContainerColor: Color, iconTint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(130.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(44.dp).background(iconContainerColor, CircleShape), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F3B66), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun DeliveryItemRow(title: String, date: String, imagePlaceholder: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = imagePlaceholder), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDF2F7)), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = date, fontSize = 12.sp, color = TextoSecundario)
            }
        }
    }
}


@Composable
fun PackagesTabScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val database = remember { FirebaseDatabase.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid ?: ""

    var listaOriginal by remember { mutableStateOf(listOf<PackageModel>()) }
    var filtroSelecionado by remember { mutableStateOf("Todas") } // "Todas", "Aguardando", "Entregues"

    // Escuta em tempo real as encomendas do usuário logado
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            database.reference.child("packages")
                .orderByChild("idUsuario").equalTo(uid)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val pacotes = mutableListOf<PackageModel>()
                        snapshot.children.forEach { child ->
                            val pacote = child.getValue(PackageModel::class.java)?.copy(id = child.key ?: "")
                            if (pacote != null) pacotes.add(pacote)
                        }
                        listaOriginal = pacotes
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
        }
    }

    // Filtra dinamicamente a lista com base na aba ativa
    val listaFiltrada = remember(listaOriginal, filtroSelecionado) {
        when (filtroSelecionado) {
            "Aguardando" -> listaOriginal.filter { it.status.lowercase() == "pendente" }
            "Entregues" -> listaOriginal.filter { it.status.lowercase() == "entregue" }
            else -> listaOriginal
        }
    }

    val azulBotao = Color(0xFF3370A6)
    val azulTextoTitulo = Color(0xFF0F3B66)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // --- TÍTULO DA TELA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = "Encomendas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = azulTextoTitulo
            )
        }

        // --- SELETOR DE ABAS (FILTROS) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todas", "Aguardando", "Entregues").forEach { aba ->
                val ativa = filtroSelecionado == aba
                Button(
                    onClick = { filtroSelecionado = aba },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ativa) azulBotao else Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = aba,
                        color = if (ativa) Color.White else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- LISTA DE ENCOMENDAS ---
        if (listaFiltrada.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma encomenda encontrada.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // Correção do Padding de início (start) e fim (end) aplicado diretamente nas margens da lista
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaFiltrada.size) { index ->
                    val pacote = listaFiltrada[index]
                    val isEntregue = pacote.status.lowercase() == "entregue"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = android.content.Intent(context, PackageDetailActivity::class.java).apply {
                                    putExtra("PACKAGE_ID", pacote.id)
                                }
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (isEntregue) Color(0xFFE1F8EB) else Color(0xFFFEF3D6),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AllInbox,
                                    contentDescription = null,
                                    tint = if (isEntregue) Color(0xFF2ECC71) else Color(0xFFF2A93B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pacote.descricao,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F3B66)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isEntregue) "Entregue" else "Aguardando",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEntregue) Color(0xFF27AE60) else Color(0xFFD35400),
                                        modifier = Modifier
                                            .background(
                                                if (isEntregue) Color(0xFFE1F8EB) else Color(0xFFFEF3D6),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• Compartimento ${pacote.compartimento}",
                                        fontSize = 12.sp,
                                        color = com.example.boxy.ui.theme.TextoSecundario
                                    )
                                }
                            }

                            // Correção da Seta: Trocado o parâmetro defeituoso pelo Modifier.rotate(180f)
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(180f) // Cria perfeitamente o efeito de seta para a direita
                            )
                        }
                    }
                }
            }
        }
    }
}