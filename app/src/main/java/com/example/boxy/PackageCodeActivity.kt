package com.example.boxy

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

class PackageCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pinAcesso = intent.getStringExtra("PIN_ACESSO") ?: "0000"
        val itemCadastrado = intent.getStringExtra("ITEM_CADASTRADO") ?: "Item"
        val qrCodeToken = intent.getStringExtra("QR_TOKEN") ?: "Sem token"

        val qrCodeBitmap = gerarQRCodeBitmap(qrCodeToken)

        setContent {
            BoxyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    PackageCodeScreen(
                        pinAcesso = pinAcesso,
                        itemCadastrado = itemCadastrado,
                        qrCodeToken = qrCodeToken,
                        qrCodeBitmap = qrCodeBitmap,
                        modifier = Modifier.padding(innerPadding),
                        onShareClick = {
                            if (qrCodeBitmap != null) {
                                compartilharTextoEQrCode(pinAcesso, itemCadastrado, qrCodeBitmap)
                            } else {
                                Toast.makeText(this, "Erro ao preparar compartilhamento", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Salva o bitmap temporariamente e dispara a Intent para compartilhar Imagem + Texto
     */
    private fun compartilharTextoEQrCode(pin: String, item: String, bitmap: Bitmap) {
        try {
            // 1. Criar um diretório temporário seguro dentro do cache do app
            val imagensFolder = File(cacheDir, "shared_images")
            imagensFolder.mkdirs()

            val arquivo = File(imagensFolder, "qrcode_entrega.png")
            val stream = FileOutputStream(arquivo)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            // 2. Obter a URI segura usando o FileProvider do seu app
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider", // Deve bater com o AndroidManifest
                arquivo
            )

            // 3. Montar a mensagem de texto
            val mensagem = "Código de acesso para entrega do meu $item na Smart Box.\n\n" +
                    "🔢 PIN: $pin\n" +
                    "📸 Escaneie o QR Code em anexo no leitor da caixa!"

            // 4. Configurar a Intent para enviar múltiplos tipos de mídia (MIME type: image/*)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri) // Adiciona a Imagem
                putExtra(Intent.EXTRA_TEXT, mensagem) // Adiciona o Texto
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Dá permissão temporária para o WhatsApp ler o arquivo
            }
            // Abre o seletor de aplicativos nativo
            startActivity(Intent.createChooser(shareIntent, "Compartilhar dados da entrega"))

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun gerarQRCodeBitmap(conteudo: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(conteudo, BarcodeFormat.QR_CODE, 512, 512)
            val largura = bitMatrix.width
            val altura = bitMatrix.height
            val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.RGB_565)

            for (x in 0 until largura) {
                for (y in 0 until altura) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// O componente PackageCodeScreen e Preview continuam exatamente iguais...
@Composable
fun PackageCodeScreen(
    pinAcesso: String,
    itemCadastrado: String,
    qrCodeToken: String,
    qrCodeBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onShareClick: () -> Unit = {}
) {
    val azulTextoTitulo = Color(0xFF0F3B66)
    val azulBotaoReal = Color(0xFF3370A6)
    val verdeSucessoFundo = Color(0xFFE1F8EB)
    val verdeSucessoIcone = Color(0xFF2ECC71)
    val cinzaBordaSutil = Color(0xFFDCDFE4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(verdeSucessoFundo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = verdeSucessoIcone, modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Encomenda cadastrada!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Compartilhe o código com o entregador", fontSize = 14.sp, color = TextoSecundario, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, cinzaBordaSutil)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "PIN de acesso", fontSize = 12.sp, color = TextoSecundario)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = pinAcesso, fontSize = 44.sp, fontWeight = FontWeight.Normal, color = azulTextoTitulo, letterSpacing = 4.sp)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.width(180.dp), color = cinzaBordaSutil)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Item cadastrado", fontSize = 12.sp, color = TextoSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = itemCadastrado, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, cinzaBordaSutil)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "QR Code", fontSize = 12.sp, color = TextoSecundario)
                Spacer(modifier = Modifier.height(16.dp))

                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code de acesso",
                        modifier = Modifier
                            .size(180.dp)
                            .border(1.dp, cinzaBordaSutil.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        Text("Erro ao gerar QR Code", color = Color.Red, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Código: $qrCodeToken", fontSize = 10.sp, color = TextoSecundario.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onShareClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = azulBotaoReal)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Compartilhar código", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}