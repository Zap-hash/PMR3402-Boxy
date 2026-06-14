package com.example.boxy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.random.Random

class RegisterPackageActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "Nova encomenda",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F3B66)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Voltar",
                                        tint = Color(0xFF0F3B66)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) { innerPadding ->
                    RegisterPackageScreen(
                        modifier = Modifier.padding(innerPadding),
                        onCadastrarClick = { descricao, compartimento, context ->
                            salvarEncomendaNoFirebase(descricao, compartimento, context)
                        }
                    )
                }
            }
        }
    }

    /**
     * Lógica central para gerar códigos, salvar no banco e navegar
     */
    private fun salvarEncomendaNoFirebase(descricao: String, compartimento: String, context: Context) {
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Gerar PIN aleatório de 4 dígitos (entre 1000 e 9999)
        val pinAleatorio = Random.nextInt(1000, 10000).toString()

        // 2. Gerar Token único para o QR Code (PIN + Timestamp atual do sistema)
        val timestamp = System.currentTimeMillis()
        val qrToken = "QR-$pinAleatorio-$timestamp"

        // 3. Criar o mapa com a estrutura exata planejada para o nó global 'packages'
        val dadosEncomenda = hashMapOf(
            "idUsuario" to userId,
            "descricao" to descricao,
            "compartimento" to compartimento,
            "status" to "pendente",
            "dataCriacao" to timestamp.toString(),
            "qrCodeToken" to qrToken
        )

        // 4. Salvar no Firebase Realtime Database sob o nó global 'packages/$pinAleatorio'
        database.reference.child("packages").child(pinAleatorio)
            .setValue(dadosEncomenda)
            .addOnSuccessListener {
                // 5. Se salvou com sucesso, vai para a tela de exibição do código gerado
                val intent = Intent(context, PackageCodeActivity::class.java).apply {
                    putExtra("PIN_ACESSO", pinAleatorio)
                    putExtra("ITEM_CADASTRADO", descricao)
                    putExtra("QR_TOKEN", qrToken)
                }
                context.startActivity(intent)

                // Finaliza esta tela para que o usuário não volte para o formulário ao apertar voltar
                finish()
            }
            .addOnFailureListener { erro ->
                Toast.makeText(context, "Erro ao salvar no banco: ${erro.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun RegisterPackageScreen(
    modifier: Modifier = Modifier,
    onCadastrarClick: (String, String, Context) -> Unit = { _, _, _ -> }
) {
    var descricaoItem by remember { mutableStateOf("") }
    var compartimentoSelecionado by remember { mutableStateOf("superior") }
    val context = LocalContext.current

    val azulBotaoReal = Color(0xFF3370A6)
    val azulTextoTitulo = Color(0xFF0F3B66)
    val cinzaBordaSutil = Color(0xFFDCDFE4)
    val azulFundoFoco = Color(0xFFE6F0FA)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- CAMPO: DESCRIÇÃO DO ITEM ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Descrição do item",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = azulTextoTitulo
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descricaoItem,
                onValueChange = { descricaoItem = it },
                singleLine = true,
                placeholder = { Text("Ex: Caixa de remédios, Notebook", color = TextoSecundario.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = azulBotaoReal,
                    unfocusedBorderColor = cinzaBordaSutil
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- SEÇÃO: SELECIONAR COMPARTIMENTO ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Selecionar compartimento",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = azulTextoTitulo
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- SELETOR COMPARTIMENTO SUPERIOR ---
            val isSuperior = compartimentoSelecionado == "superior"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSuperior) azulFundoFoco else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isSuperior) 2.dp else 1.dp,
                        color = if (isSuperior) azulBotaoReal else cinzaBordaSutil,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { compartimentoSelecionado = "superior" }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isSuperior) azulBotaoReal else Color(0xFFF0F2F5),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AllInbox,
                        contentDescription = null,
                        tint = if (isSuperior) Color.White else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Compartimento superior",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulTextoTitulo
                    )
                    Text(
                        text = "Entrada direta pela abertura superior",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- SELETOR COMPARTIMENTO INFERIOR ---
            val isInferior = compartimentoSelecionado == "inferior"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isInferior) azulFundoFoco else Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isInferior) 2.dp else 1.dp,
                        color = if (isInferior) azulBotaoReal else cinzaBordaSutil,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { compartimentoSelecionado = "inferior" }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isInferior) azulBotaoReal else Color(0xFFF0F2F5),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AllInbox,
                        contentDescription = null,
                        tint = if (isInferior) Color.White else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Compartimento inferior",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulTextoTitulo
                    )
                    Text(
                        text = "Recomendado para itens frágeis ou alimentos",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // --- BOTÃO PRINCIPAL: GERAR CÓDIGO E CADASTRAR ---
        Button(
            onClick = {
                // Validação simples: não deixa cadastrar sem preencher a descrição
                if (descricaoItem.isNotBlank()) {
                    onCadastrarClick(descricaoItem.trim(), compartimentoSelecionado, context)
                } else {
                    Toast.makeText(context, "Por favor, preencha a descrição do item.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = azulBotaoReal)
        ) {
            Text(
                text = "Gerar código e cadastrar",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterPackageScreenPreview() {
    BoxyTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                CenterAlignedTopAppBar(
                    title = { Text("Nova encomenda", color = Color(0xFF0F3B66), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF0F3B66))
                        }
                    }
                )
            }
        ) { innerPadding ->
            RegisterPackageScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}