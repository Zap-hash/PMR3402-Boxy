package com.example.boxy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.boxy.ui.theme.BoxyTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoxyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SplashScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAnimationFinished = {
                            // Por enquanto, navega para a LoginActivity.
                            // No futuro, aqui leremos o DataStore para decidir entre Login ou Tela Principal.
                            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Fecha a Splash para o usuário não voltar para ela ao apertar o botão "Voltar"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    // Carrega o arquivo JSON do Lottie da pasta res/raw
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.animacao_splash) // Substitua pelo nome exato do seu arquivo .json
    )

    // Controla o progresso da animação
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1 // Executa a animação apenas 1 vez antes de mudar de tela
    )

    // Efeito lançado para monitorar quando a animação chega ao fim
    LaunchedEffect(progress) {
        if (progress == 1f) {
            // Pequena pausa imperceptível para suavizar a transição de tela
            delay(100)
            onAnimationFinished()
        }
    }

    // Tela de fundo com alinhamento centralizado
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White), // Fundo branco puro conforme solicitado
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp) // Altere o tamanho aqui se quiser a animação maior ou menor
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    BoxyTheme {
        SplashScreen()
    }
}