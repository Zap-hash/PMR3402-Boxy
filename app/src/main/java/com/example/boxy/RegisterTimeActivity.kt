package com.example.boxy

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.boxy.ui.theme.BoxyTheme
import com.example.boxy.ui.theme.TextoSecundario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

data class DiaSemanaItem(val id: Int, val letra: String)
class RegisterTimeActivity : ComponentActivity() {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BoxyTheme {
                val userId = auth.currentUser?.uid ?: ""

                // Estados para monitorar o horário vindo do Firebase
                var temHorarioSalvo by remember { mutableStateOf(false) }
                var stringInicioSalva by remember { mutableStateOf("") }
                var stringFimSalva by remember { mutableStateOf("") }
                var carregandoDados by remember { mutableStateOf(true) }

                // Escuta em tempo real se já existe configuração no Firebase
                LaunchedEffect(userId) {
                    if (userId.isNotBlank()) {
                        database.reference.child("schedules").child(userId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val configurado = snapshot.child("configurado").getValue(Boolean::class.java) ?: false
                                    if (configurado) {
                                        temHorarioSalvo = true
                                        stringInicioSalva = snapshot.child("regras/regra_principal/stringInicio").getValue(String::class.java) ?: ""
                                        stringFimSalva = snapshot.child("regras/regra_principal/stringFim").getValue(String::class.java) ?: ""
                                    } else {
                                        temHorarioSalvo = false
                                    }
                                    carregandoDados = false
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    carregandoDados = false
                                }
                            })
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Horários de abertura", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF0F3B66))
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                        )
                    }
                ) { innerPadding ->
                    if (carregandoDados) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF3370A6))
                        }
                    } else {
                        RegisterTimeScreen(
                            modifier = Modifier.padding(innerPadding),
                            temHorarioSalvo = temHorarioSalvo,
                            stringInicioSalva = stringInicioSalva,
                            stringFimSalva = stringFimSalva,
                            onSalvarClick = { horaInicio, horaFim, diasSelecionados, context ->
                                salvarHorarioNoFirebase(horaInicio, horaFim, diasSelecionados, context)
                            },
                            onDeletarClick = { context ->
                                deletarHorarioNoFirebase(context)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun salvarHorarioNoFirebase(horaInicio: String, horaFim: String, diasSelecionados: Set<Int>, context: Context) {
        val userId = auth.currentUser?.uid ?: return

        val calcularMinutos = { horaString: String ->
            val limpa = horaString.replace(":", "")
            if (limpa.length == 4) {
                val h = limpa.substring(0, 2).toInt()
                val m = limpa.substring(2, 4).toInt()
                Pair((h * 60) + m, "$h:${limpa.substring(2, 4)}")
            } else Pair(0, "00:00")
        }

        val (inicioMinutos, stringInicioFormatada) = calcularMinutos(horaInicio)
        val (fimMinutos, stringFimFormatada) = calcularMinutos(horaFim)

        // --- VALIDAÇÃO DE TEMPO PRESENTE / FUTURO ---
        val calendario = Calendar.getInstance()
        val horaAtual = calendario.get(Calendar.HOUR_OF_DAY)
        val minutoAtual = calendario.get(Calendar.MINUTE)
        val minutosAtuaisDoDia = (horaAtual * 60) + minutoAtual
        val diaDaSemanaAtual = calendario.get(Calendar.DAY_OF_WEEK) // 1=Dom, 2=Seg...

        // Se a regra incluir o dia de HOJE, o horário de fim não pode ter passado
        if (diasSelecionados.contains(diaDaSemanaAtual) && fimMinutos <= minutosAtuaisDoDia) {
            Toast.makeText(context, "🚫 Erro: O horário de término já passou no dia de hoje!", Toast.LENGTH_LONG).show()
            return
        }

        val diasMap = hashMapOf<String, Boolean>()
        diasSelecionados.forEach { diasMap[it.toString()] = true }

        val regraHorario = hashMapOf(
            "horarioInicioMinutos" to inicioMinutos,
            "horarioFimMinutos" to fimMinutos,
            "stringInicio" to stringInicioFormatada,
            "stringFim" to stringFimFormatada,
            "diasSemana" to diasMap
        )

        val dadosSchedules = hashMapOf(
            "configurado" to true,
            "regras" to hashMapOf("regra_principal" to regraHorario)
        )

        database.reference.child("schedules").child(userId)
            .setValue(dadosSchedules)
            .addOnSuccessListener {
                Toast.makeText(context, "Horário ativo cadastrado!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deletarHorarioNoFirebase(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("schedules").child(userId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Horário removido com sucesso", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun RegisterTimeScreen(
    modifier: Modifier = Modifier,
    temHorarioSalvo: Boolean,
    stringInicioSalva: String,
    stringFimSalva: String,
    onSalvarClick: (String, String, Set<Int>, Context) -> Unit,
    onDeletarClick: (Context) -> Unit
) {
    val context = LocalContext.current
    var exibindoFormulario by remember { mutableStateOf(false) }

    // Estados internos capturam apenas os números digitados (máx 4 caracteres)
    var digitosInicio by remember { mutableStateOf("0900") }
    var digitosFim by remember { mutableStateOf("1800") }

    var diasSelecionados by remember { mutableStateOf(setOf<Int>()) }
    val listaDias = remember {
        listOf(
            DiaSemanaItem(1, "D"), DiaSemanaItem(2, "S"), DiaSemanaItem(3, "T"),
            DiaSemanaItem(4, "Q"), DiaSemanaItem(5, "Q"), DiaSemanaItem(6, "S"), DiaSemanaItem(7, "S")
        )
    }

    val azulBotaoReal = Color(0xFF3370A6)
    val azulTextoTitulo = Color(0xFF0F3B66)
    val cinzaBordaSutil = Color(0xFFDCDFE4)
    val cinzaFundoBotaoInativo = Color(0xFFF0F2F5)
    val azulFundoFoco = Color(0xFFE6F0FA)

    // Função auxiliar para aplicar a máscara visual de 4 dígitos para XX:XX
    val aplicarMascaraHorario = { texto: String ->
        val limpo = texto.padEnd(4, '0')
        "${limpo.substring(0, 2)}:${limpo.substring(2, 4)}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- MODIFICAÇÃO 2: CARD DINÂMICO BASEADO NO BANCO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, cinzaBordaSutil)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!temHorarioSalvo) {
                    // Estado Vazio (Original)
                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFF0F2F5), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Nenhum horário configurado", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Configure horários para abertura\nautomática da caixa", fontSize = 12.sp, color = TextoSecundario, textAlign = TextAlign.Center)
                } else {
                    // Estado Ativo: Substitui o aviso pelo horário real do Firebase!
                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFE1F8EB), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.AccessTime, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Abertura Automática Ativa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F3B66))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Das $stringInicioSalva até às $stringFimSalva",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = azulBotaoReal
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onDeletarClick(context) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Regra", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!exibindoFormulario) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(width = 1.5.dp, color = azulBotaoReal, shape = RoundedCornerShape(12.dp))
                    .clickable { exibindoFormulario = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = azulBotaoReal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Configurar novo horário", color = azulBotaoReal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            // Formulário ativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, cinzaBordaSutil)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(text = "Novo horário", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = azulTextoTitulo)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // --- MODIFICAÇÃO 1: INPUT ESTILO REGISTRADOR DE DESLOCAMENTO ---
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Horário início", fontSize = 12.sp, color = TextoSecundario)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = aplicarMascaraHorario(digitosInicio),
                                onValueChange = { input ->
                                    // Remove tudo que não for número
                                    val apenasNumeros = input.filter { it.isDigit() }
                                    if (apenasNumeros.length <= 4) {
                                        digitosInicio = apenasNumeros
                                    } else {
                                        // Substituição à direita se passar de 4 (comportamento de shift)
                                        digitosInicio = apenasNumeros.takeLast(4)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = azulBotaoReal, unfocusedBorderColor = cinzaBordaSutil)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Horário fim", fontSize = 12.sp, color = TextoSecundario)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = aplicarMascaraHorario(digitosFim),
                                onValueChange = { input ->
                                    val apenasNumeros = input.filter { it.isDigit() }
                                    if (apenasNumeros.length <= 4) {
                                        digitosFim = apenasNumeros
                                    } else {
                                        digitosFim = apenasNumeros.takeLast(4)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = azulBotaoReal, unfocusedBorderColor = cinzaBordaSutil)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Dias da semana", fontSize = 12.sp, color = TextoSecundario)
                        Text(text = "Todos os dias", fontSize = 12.sp, color = azulBotaoReal, modifier = Modifier.clickable {
                            diasSelecionados = if (diasSelecionados.size == 7) emptySet() else (1..7).toSet()
                        })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listaDias.forEach { dia ->
                            val selecionado = diasSelecionados.contains(dia.id)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color = if (selecionado) azulFundoFoco else cinzaFundoBotaoInativo, shape = CircleShape)
                                    .border(width = if (selecionado) 1.5.dp else 0.dp, color = if (selecionado) azulBotaoReal else Color.Transparent, shape = CircleShape)
                                    .clickable { diasSelecionados = if (selecionado) diasSelecionados - dia.id else diasSelecionados + dia.id },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = dia.letra, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selecionado) azulBotaoReal else Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { exibindoFormulario = false },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cinzaBordaSutil)
                        ) {
                            Text(text = "Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (diasSelecionados.isEmpty()) {
                                    Toast.makeText(context, "Selecione os dias da semana!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (digitosInicio.length < 4 || digitosFim.length < 4) {
                                    Toast.makeText(context, "Digite os horários completos!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onSalvarClick(digitosInicio, digitosFim, diasSelecionados, context)
                                exibindoFormulario = false
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = azulBotaoReal)
                        ) {
                            Text(text = "Salvar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}