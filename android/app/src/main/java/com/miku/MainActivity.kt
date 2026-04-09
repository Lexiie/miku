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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val AgentBubble = Color(0xFF121B2A)
private val UserBubble = Color(0xFF19352E)
private val SuccessBubble = Color(0xFF112A22)
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

private data class QuickAction(
    val label: String,
    val command: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private data class SystemStatus(
    val title: String,
    val explanation: String,
    val details: String?,
)

private enum class BubbleStyle {
    User,
    Agent,
    Success,
}

/** Android entrypoint for the Compose chat client. */
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

/**
 * Root screen for connection setup, message timeline, system status cards, and composer.
 */
@Composable
fun ChatScreen(executor: AutomationExecutor) {
    val viewModel: ChatViewModel = viewModel()
    val context = LocalContext.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var inputText by rememberSaveable { mutableStateOf("") }
    var endpointUrl by rememberSaveable { mutableStateOf(viewModel.agentUrl) }
    var composerHeightPx by rememberSaveable { mutableStateOf(0) }
    var showConnectionEditor by rememberSaveable { mutableStateOf(false) }

    val quickActions = remember {
        listOf(
            QuickAction("Set Alarm", "set alarm for 7 AM", Icons.Default.Alarm),
            QuickAction("Open Camera", "open camera", Icons.Default.CameraAlt),
            QuickAction("Call", "call +1 555 0100", Icons.Default.Call),
            QuickAction("Flashlight", "turn on flashlight", Icons.Default.FlashlightOn),
        )
    }

    // Voice dictation populates composer and optionally sends immediately when connected.
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

    // Keep latest message visible as status/action updates arrive.
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

    LaunchedEffect(viewModel.isConnected) {
        if (viewModel.isConnected) {
            showConnectionEditor = false
        }
    }

    // Dynamic inset prevents the bottom composer from covering recent messages.
    val composerBottomInset = with(density) { composerHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1322), Midnight, Color(0xFF060910)),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x222AA879), Color.Transparent),
                        radius = 840f,
                    ),
                ),
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                MikuTopBar(
                    isConnected = viewModel.isConnected,
                    isConnecting = viewModel.isConnecting,
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    ConnectionCard(
                        endpointUrl = endpointUrl,
                        onEndpointChange = { endpointUrl = it },
                        isConnected = viewModel.isConnected,
                        isConnecting = viewModel.isConnecting,
                        connectedUrl = viewModel.agentUrl,
                        expanded = showConnectionEditor || !viewModel.isConnected,
                        onExpandToggle = { showConnectionEditor = !showConnectionEditor },
                        onConnect = {
                            if (!viewModel.isConnecting) {
                                val trimmed = endpointUrl.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.connect(trimmed)
                                }
                            }
                        },
                        onDisconnect = { viewModel.disconnect() },
                    )

                    if (viewModel.isConnected) {
                        Spacer(Modifier.height(10.dp))
                        QuickActionsRow(
                            actions = quickActions,
                            enabled = !viewModel.isConnecting,
                            onActionClick = { quickAction ->
                                viewModel.sendMessage(quickAction.command, executor)
                            },
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (viewModel.messages.isEmpty()) {
                            EmptyStateCard(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 8.dp),
                                isConnected = viewModel.isConnected,
                                suggestions = quickActions.map { it.command },
                                onSuggestionClick = { suggestion ->
                                    if (viewModel.isConnected) {
                                        viewModel.sendMessage(suggestion, executor)
                                    } else {
                                        inputText = suggestion
                                    }
                                },
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = composerBottomInset + 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item { Spacer(Modifier.height(4.dp)) }
                                itemsIndexed(viewModel.messages) { index, message ->
                                    val retryCommand = lastUserCommand(viewModel.messages, index)
                                    val systemStatus = parseSystemStatus(message)
                                    if (systemStatus != null) {
                                        SystemStatusCard(
                                            status = systemStatus,
                                            primaryActionLabel = if (viewModel.isConnected) "Retry" else "Reconnect",
                                            onPrimaryAction = {
                                                if (viewModel.isConnecting) {
                                                    return@SystemStatusCard
                                                }
                                                if (viewModel.isConnected) {
                                                    if (retryCommand.isNotBlank()) {
                                                        viewModel.sendMessage(retryCommand, executor)
                                                    } else {
                                                        val retryText = inputText.trim()
                                                        if (retryText.isBlank()) {
                                                            return@SystemStatusCard
                                                        }
                                                        viewModel.sendMessage(retryText, executor)
                                                    }
                                                } else {
                                                    if (viewModel.isConnecting) {
                                                        return@SystemStatusCard
                                                    }
                                                    if (endpointUrl.isBlank()) {
                                                        showConnectionEditor = true
                                                    } else {
                                                        viewModel.connect(endpointUrl)
                                                    }
                                                }
                                            },
                                        )
                                    } else {
                                        ChatMessageItem(message)
                                    }
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }

                BottomComposer(
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
                    enabled = viewModel.isConnected,
                    isConnecting = viewModel.isConnecting,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { composerHeightPx = it.height },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MikuTopBar(
    isConnected: Boolean,
    isConnecting: Boolean,
) {
    // Top bar intentionally stays compact; detailed endpoint controls live below.
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Mist,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A7CFFB2)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = "Miku",
                    style = MaterialTheme.typography.titleLarge,
                    color = Mist,
                )
            }
        },
        actions = {
            StatusChip(
                isConnected = isConnected,
                isConnecting = isConnecting,
                modifier = Modifier.padding(end = 6.dp),
            )
        },
    )
}

/** Compact connection status indicator used in app bar. */
@Composable
private fun StatusChip(
    isConnected: Boolean,
    isConnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = when {
        isConnecting -> Color(0xFFFFD36E)
        isConnected -> Accent
        else -> ErrorTint
    }

    val label = when {
        isConnecting -> "Connecting"
        isConnected -> "Connected"
        else -> "Offline"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color(0xCC0F1726),
        border = BorderStroke(1.dp, BorderTint),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                color = Mist,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Endpoint setup panel.
 *
 * Connected state collapses into a summary row and can be expanded for editing.
 */
@Composable
private fun ConnectionCard(
    endpointUrl: String,
    onEndpointChange: (String) -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    connectedUrl: String,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isConnected && !expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connected endpoint",
                            style = MaterialTheme.typography.labelLarge,
                            color = Mist,
                        )
                        Text(
                            text = connectedUrl.ifBlank { endpointUrl },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedMist,
                            maxLines = 1,
                        )
                    }

                    TextButton(onClick = onExpandToggle) {
                        Text("Edit")
                    }
                }
            } else {
                Text(
                    text = "Agent endpoint",
                    style = MaterialTheme.typography.labelLarge,
                    color = Mist,
                )
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = onEndpointChange,
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
                        focusedContainerColor = Color(0xFF0C1524),
                        unfocusedContainerColor = Color(0xFF0C1524),
                        disabledContainerColor = Color(0xFF0A1220),
                        focusedTextColor = Mist,
                        unfocusedTextColor = Mist,
                        cursorColor = Accent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onConnect,
                        enabled = !isConnecting && endpointUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Midnight,
                        ),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Midnight,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isConnected) "Reconnect" else "Connect")
                    }

                    if (isConnected) {
                        FilledTonalButton(
                            onClick = onDisconnect,
                            enabled = !isConnecting,
                        ) {
                            Text("Disconnect")
                        }
                    }

                    if (isConnected) {
                        TextButton(onClick = onExpandToggle) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Hide")
                        }
                    }
                }
            }

            if (isConnected && !expanded) {
                Divider(color = BorderTint.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Connection settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedMist,
                    )
                    TextButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Expand")
                    }
                }
            }
        }
    }
}

/** Command shortcuts shown only while connected. */
@Composable
private fun QuickActionsRow(
    actions: List<QuickAction>,
    enabled: Boolean,
    onActionClick: (QuickAction) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(actions) { action ->
            AssistChip(
                onClick = { onActionClick(action) },
                enabled = enabled,
                label = { Text(action.label) },
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFF121C2B),
                    labelColor = Mist,
                    leadingIconContentColor = Accent,
                    disabledContainerColor = Color(0xFF101827),
                    disabledLabelColor = MutedMist,
                    disabledLeadingIconContentColor = MutedMist,
                ),
                border = BorderStroke(1.dp, BorderTint),
            )
        }
    }
}

/** Minimal empty state to keep chat area visually dominant. */
@Composable
private fun EmptyStateCard(
    modifier: Modifier = Modifier,
    isConnected: Boolean,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xCC0F1726),
        border = BorderStroke(1.dp, BorderTint),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isConnected) "Command console ready" else "Connect your endpoint to start",
                style = MaterialTheme.typography.titleMedium,
                color = Mist,
            )
            Text(
                text = if (isConnected) {
                    "Send a natural command and Miku will execute native Android actions."
                } else {
                    "Once connected, you can run alarms, calls, app controls, and more."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedMist,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions.take(3)) { suggestion ->
                    AssistChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF141E2E),
                            labelColor = Mist,
                        ),
                        border = BorderStroke(1.dp, BorderTint),
                    )
                }
            }
        }
    }
}

/** Dedicated non-chat card for actionable system failures/timeouts. */
@Composable
private fun SystemStatusCard(
    status: SystemStatus,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by rememberSaveable(status.details) { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2A1620),
        border = BorderStroke(1.dp, Color(0xFF5B2A37)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFFFD6DD),
            )
            Text(
                text = status.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFE7EB),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7CFFB2),
                        contentColor = Midnight,
                    ),
                ) {
                    Text(primaryActionLabel)
                }

                if (!status.details.isNullOrBlank()) {
                    TextButton(onClick = { showDetails = !showDetails }) {
                        Text(if (showDetails) "Hide details" else "Show details")
                    }
                }
            }

            if (showDetails && !status.details.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1020),
                    border = BorderStroke(1.dp, Color(0xFF463047)),
                ) {
                    Text(
                        text = status.details,
                        color = Color(0xFFE8CFD6),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }
    }
}

/** Bottom-anchored composer with input, voice, and send actions. */
@Composable
private fun BottomComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    isConnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xF20C1220),
        border = BorderStroke(1.dp, BorderTint),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    maxLines = 3,
                    enabled = enabled && !isConnecting,
                    placeholder = {
                        Text(
                            text = if (enabled) "Try: set alarm for 7 AM" else "Connect to start",
                            color = MutedMist,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentSoft,
                        unfocusedBorderColor = BorderTint,
                        focusedContainerColor = Color(0xFF0D1728),
                        unfocusedContainerColor = Color(0xFF0D1728),
                        disabledContainerColor = Color(0xFF0A1220),
                        focusedTextColor = Mist,
                        unfocusedTextColor = Mist,
                        cursorColor = Accent,
                    ),
                    shape = RoundedCornerShape(14.dp),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(
                        onClick = onMicClick,
                        enabled = enabled && !isConnecting,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = Mist,
                        )
                    }

                    Button(
                        onClick = onSendClick,
                        enabled = enabled && inputText.isNotBlank() && !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Midnight,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (enabled) {
                Text(
                    text = "Try: open Spotify",
                    color = MutedMist,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                )
            }
        }
    }
}

/** Chat bubble renderer with simplified visual variants. */
@Composable
private fun ChatMessageItem(message: Message) {
    val bubbleStyle = resolveBubbleStyle(message)
    val bubbleColor = when (bubbleStyle) {
        BubbleStyle.User -> UserBubble
        BubbleStyle.Agent -> AgentBubble
        BubbleStyle.Success -> SuccessBubble
    }

    val borderColor = when (bubbleStyle) {
        BubbleStyle.User -> Color(0xFF2A5A4D)
        BubbleStyle.Agent -> BorderTint
        BubbleStyle.Success -> Color(0xFF2E5F4D)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 336.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                )
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedMist,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/** Distinguishes agent success updates from regular assistant text. */
private fun resolveBubbleStyle(message: Message): BubbleStyle {
    if (message.isUser) {
        return BubbleStyle.User
    }

    val lower = message.text.lowercase(Locale.getDefault())
    return if (lower.startsWith("⚡") && lower.contains("✅")) {
        BubbleStyle.Success
    } else {
        BubbleStyle.Agent
    }
}

/** Maps backend/system error strings into structured UI status cards. */
private fun parseSystemStatus(message: Message): SystemStatus? {
    if (message.isUser) {
        return null
    }

    val text = message.text.trim()
    val lower = text.lowercase(Locale.getDefault())
    val isFailure =
        text.startsWith("❌") ||
            lower.contains("error:") ||
            lower.contains("failed") ||
            lower.contains("timeout")
    if (!isFailure) {
        return null
    }

    return when {
        lower.contains("timeout") -> SystemStatus(
            title = "Request timed out",
            explanation = "Miku could not get a response in time. Check endpoint health and try again.",
            details = text,
        )
        lower.contains("connection") || lower.contains("health check") -> SystemStatus(
            title = "Connection issue",
            explanation = "Miku cannot reach the configured endpoint. Verify URL and network status.",
            details = text,
        )
        lower.contains("not connected") -> SystemStatus(
            title = "Not connected",
            explanation = "Connect to your agent endpoint before sending commands.",
            details = null,
        )
        else -> SystemStatus(
            title = "Command failed",
            explanation = "The request could not be completed. Try once more or inspect details.",
            details = text,
        )
    }
}

/** Finds most recent user command before a given message index for retry actions. */
private fun lastUserCommand(messages: List<Message>, fromIndex: Int): String {
    for (index in (fromIndex - 1) downTo 0) {
        val candidate = messages[index]
        if (candidate.isUser) {
            return candidate.text.trim()
        }
    }
    return ""
}

/** Consistent HH:mm timestamp formatting for chat messages. */
private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
