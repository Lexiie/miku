package com.miku.agent

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Midnight = Color(0xFF070B14)
private val DeepNavy = Color(0xFF10192B)
private val Velvet = Color(0xFF172338)
private val Mist = Color(0xFFE7EEF8)
private val MutedMist = Color(0xFF9CA8BF)
private val Accent = Color(0xFF7CFFB2)
private val AccentSoft = Color(0xFF35D9A0)
private val AgentBubble = Color(0xFF141D2E)
private val UserBubble = Color(0xFF1D3A33)
private val BorderTint = Color(0xFF24324B)
private val ErrorTint = Color(0xFFFF7070)

private val MikuDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Midnight,
    secondary = Color(0xFF8ED6FF),
    onSecondary = Midnight,
    background = Midnight,
    onBackground = Mist,
    surface = DeepNavy,
    onSurface = Mist,
    surfaceVariant = Velvet,
    onSurfaceVariant = MutedMist,
    outline = BorderTint,
    tertiary = Color(0xFFFFD36E),
    error = ErrorTint,
)

private val MikuTypography = Typography().copy(
    headlineMedium = Typography().headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = Typography().bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

class MainActivity : ComponentActivity() {
    private lateinit var executor: AutomationExecutor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = AutomationExecutor(this)

        setContent {
            MaterialTheme(
                colorScheme = MikuDarkColors,
                typography = MikuTypography,
            ) {
                ChatScreen(executor)
            }
        }
    }
}

@Composable
fun ChatScreen(executor: AutomationExecutor) {
    val viewModel: ChatViewModel = viewModel()
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var endpointUrl by rememberSaveable { mutableStateOf(viewModel.agentUrl) }
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (spokenText.isNotEmpty()) {
            inputText = spokenText
            if (viewModel.isConnected) {
                viewModel.sendMessage(spokenText, executor)
                inputText = ""
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your Android command")
            }
            speechLauncher.launch(intent)
        }
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size)
        }
    }

    LaunchedEffect(viewModel.agentUrl) {
        if (viewModel.agentUrl.isNotBlank()) {
            endpointUrl = viewModel.agentUrl
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF08111D), Midnight, Color(0xFF05070D)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF20434F), Color.Transparent),
                        radius = 900f,
                    ),
                ),
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HeaderPanel(
                    endpointUrl = endpointUrl,
                    onEndpointChange = { endpointUrl = it },
                    isConnected = viewModel.isConnected,
                    isConnecting = viewModel.isConnecting,
                    connectedUrl = viewModel.agentUrl,
                    onConnectToggle = {
                        if (viewModel.isConnected) {
                            viewModel.disconnect()
                        } else if (endpointUrl.isNotBlank()) {
                            viewModel.connect(endpointUrl)
                        }
                    },
                )
            },
            bottomBar = {
                ComposerBar(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    onMicClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onSendClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.sendMessage(trimmed, executor)
                            inputText = ""
                        }
                    },
                    enabled = viewModel.isConnected && !viewModel.isConnecting,
                    isConnecting = viewModel.isConnecting,
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                if (viewModel.messages.isEmpty()) {
                    EmptyConversationState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        isConnected = viewModel.isConnected,
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(Modifier.height(12.dp)) }
                    items(viewModel.messages) { message ->
                        MessageBubble(message)
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HeaderPanel(
    endpointUrl: String,
    onEndpointChange: (String) -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    connectedUrl: String,
    onConnectToggle: () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "MIKU CONSOLE",
                            color = MutedMist,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 1.6.sp,
                        )
                    }
                    Text(
                        text = "Elegant Android control,\ndelivered in real time",
                        color = Mist,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = if (isConnected) {
                            "Connected and ready to execute native device actions."
                        } else {
                            "Connect your agent endpoint from the header, then start issuing commands."
                        },
                        color = MutedMist,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                StatusPill(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xCC10192B),
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, BorderTint),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x1A7CFFB2)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Accent,
                            )
                        }
                        Column {
                            Text(
                                text = "Agent Endpoint",
                                color = Mist,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (isConnected) connectedUrl else "Paste your ElizaOS or Nosana URL here",
                                color = MutedMist,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = endpointUrl,
                            onValueChange = onEndpointChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isConnecting,
                            placeholder = {
                                Text(
                                    text = "https://your-agent-url",
                                    color = MutedMist,
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentSoft,
                                unfocusedBorderColor = BorderTint,
                                focusedContainerColor = Color(0xFF0B1322),
                                unfocusedContainerColor = Color(0xFF0B1322),
                                disabledContainerColor = Color(0xFF0B1322),
                                focusedTextColor = Mist,
                                unfocusedTextColor = Mist,
                                cursorColor = Accent,
                            ),
                            shape = RoundedCornerShape(20.dp),
                        )

                        FilledIconButton(
                            onClick = onConnectToggle,
                            modifier = Modifier.size(56.dp),
                            enabled = !isConnecting && (isConnected || endpointUrl.isNotBlank()),
                            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isConnected) Color(0xFF3B1D22) else Accent,
                                contentColor = if (isConnected) Color(0xFFFFC8CC) else Midnight,
                                disabledContainerColor = Color(0xFF172235),
                                disabledContentColor = MutedMist,
                            ),
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Mist,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    isConnected: Boolean,
    isConnecting: Boolean,
) {
    val dotColor = when {
        isConnecting -> Color(0xFFFFD36E)
        isConnected -> Accent
        else -> ErrorTint
    }
    val label = when {
        isConnecting -> "Linking"
        isConnected -> "Online"
        else -> "Offline"
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xB3152132),
        border = BorderStroke(1.dp, BorderTint),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                color = Mist,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun EmptyConversationState(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD0121A2A),
        border = BorderStroke(1.dp, BorderTint),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF173F39), Color(0xFF0D1726)),
                        ),
                    )
                    .border(1.dp, Color(0xFF2C544C), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(30.dp),
                )
            }

            Text(
                text = if (isConnected) "Say less. Control more." else "Your control room is ready.",
                color = Mist,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (isConnected) {
                    "Try commands like setting an alarm, opening Spotify, changing brightness, or sending a reminder."
                } else {
                    "Add your agent URL in the header, connect, and Miku will route every request into native Android actions."
                },
                color = MutedMist,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ComposerBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    isConnecting: Boolean,
) {
    Surface(
        color = Color(0xEE0C1220),
        border = BorderStroke(1.dp, BorderTint),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (enabled) "Type an Android action..." else "Connect to start chatting...",
                        color = MutedMist,
                    )
                },
                enabled = enabled,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentSoft,
                    unfocusedBorderColor = BorderTint,
                    focusedContainerColor = Color(0xFF0C1627),
                    unfocusedContainerColor = Color(0xFF0C1627),
                    disabledContainerColor = Color(0xFF0A1220),
                    focusedTextColor = Mist,
                    unfocusedTextColor = Mist,
                    cursorColor = Accent,
                ),
                shape = RoundedCornerShape(22.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledIconButton(
                    onClick = onMicClick,
                    enabled = enabled,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF162338),
                        contentColor = Mist,
                        disabledContainerColor = Color(0xFF101725),
                        disabledContentColor = MutedMist,
                    ),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice")
                }

                FilledIconButton(
                    onClick = onSendClick,
                    enabled = enabled && inputText.isNotBlank() && !isConnecting,
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = Accent,
                        contentColor = Midnight,
                        disabledContainerColor = Color(0xFF172235),
                        disabledContentColor = MutedMist,
                    ),
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!message.isUser) {
            BubbleBadge(
                icon = Icons.Default.Bolt,
                tint = Accent,
                container = Color(0xFF122033),
            )
            Spacer(Modifier.width(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = if (message.isUser) 24.dp else 8.dp,
                bottomEnd = if (message.isUser) 8.dp else 24.dp,
            ),
            color = if (message.isUser) UserBubble else AgentBubble,
            border = BorderStroke(1.dp, if (message.isUser) Color(0xFF285348) else BorderTint),
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (message.isUser) "YOU" else "MIKU",
                    color = if (message.isUser) Color(0xFFB8F7DA) else MutedMist,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.3.sp,
                )
                Text(
                    text = message.text,
                    color = Mist,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    color = MutedMist,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        if (message.isUser) {
            Spacer(Modifier.width(10.dp))
            BubbleBadge(
                icon = Icons.Default.AutoAwesome,
                tint = Color(0xFFB8F7DA),
                container = Color(0xFF163027),
            )
        }
    }
}

@Composable
private fun BubbleBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
