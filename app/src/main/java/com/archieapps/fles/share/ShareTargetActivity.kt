package com.archieapps.fles.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.archieapps.fles.FlesApp
import com.archieapps.fles.MainActivity
import com.archieapps.fles.di.AppContainer
import com.archieapps.fles.ui.components.AppButton
import com.archieapps.fles.ui.screens.ShareTargetScreen
import com.archieapps.fles.ui.theme.FlesTheme
import com.archieapps.fles.ui.viewmodel.SharePayload

class ShareTargetActivity : ComponentActivity() {

    private val viewModel by lazy {
        val container = appContainer()
        ShareTargetViewModel(
            channelsRepository = container.channelsRepository,
            messagesRepository = container.messagesRepository,
            secureFoldersRepository = container.secureFoldersRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val payload = parsePayload(intent)
        if (payload == null) {
            Toast.makeText(this, "Nada para compartilhar.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            FlesTheme {
                var authChecked by remember { mutableStateOf<Boolean?>(null) }
                LaunchedEffect(Unit) {
                    val token = appContainer().authRepository.getSavedToken()
                    authChecked = !token.isNullOrBlank()
                    if (authChecked == true) {
                        viewModel.setPayload(payload)
                    }
                }
                when (authChecked) {
                    null -> LoadingBox()
                    false -> NotAuthenticatedScreen(
                        onOpenApp = {
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            finish()
                        },
                        onClose = ::finish
                    )
                    true -> {
                        val state by viewModel.state.collectAsState()
                        ShareTargetScreen(
                            state = state,
                            onQueryChange = viewModel::onQueryChange,
                            onSend = { channelId ->
                                viewModel.sendTo(channelId) { finish() }
                            },
                            onClose = ::finish
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val payload = parsePayload(intent) ?: return
        viewModel.setPayload(payload)
    }

    private fun parsePayload(source: Intent?): SharePayload? {
        return SharePayload.fromIntent(source, appContainer().attachmentIo)
    }

    private fun appContainer(): AppContainer = (application as FlesApp).container
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlesTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = FlesTheme.colors.primary)
    }
}

@Composable
private fun NotAuthenticatedScreen(onOpenApp: () -> Unit, onClose: () -> Unit) {
    val colors = FlesTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Faça login para compartilhar",
            color = colors.foreground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        Text(
            "Abra o app e entre na sua conta para receber arquivos compartilhados.",
            color = colors.mutedForeground,
            fontSize = 14.sp
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))
        AppButton("Abrir o app", onClick = onOpenApp)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        AppButton("Fechar", onClick = onClose, variant = com.archieapps.fles.ui.components.AppButtonVariant.Ghost)
    }
}
