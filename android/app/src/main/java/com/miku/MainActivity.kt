package com.miku.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var executor: AutomationExecutor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = AutomationExecutor(this)
        
        setContent {
            MaterialTheme {
                ChatScreen(executor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(executor: AutomationExecutor) {
    val viewModel: ChatViewModel = viewModel()
    var inputText by remember { mutableStateOf("") }
    var endpointUrl by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 Miku") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (viewModel.isConnected) Color.Green else Color.Red,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                    Spacer(Modifier.width(16.dp))
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Endpoint input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Agent URL") },
                    placeholder = { Text("https://xxx.nos.ci") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (viewModel.isConnected) {
                            viewModel.disconnect()
                        } else if (endpointUrl.isNotEmpty()) {
                            viewModel.connect(endpointUrl)
                        }
                    }
                ) {
                    Text(if (viewModel.isConnected) "Disconnect" else "Connect")
                }
            }
            
            Divider()
            
            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                state = listState
            ) {
                items(viewModel.messages) { message ->
                    MessageBubble(message)
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            Divider()
            
            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type or tap mic...") },
                    modifier = Modifier.weight(1f),
                    enabled = viewModel.isConnected
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        // TODO: Voice input
                    },
                    enabled = viewModel.isConnected
                ) {
                    Icon(Icons.Default.Mic, "Voice")
                }
                IconButton(
                    onClick = {
                        if (inputText.isNotEmpty()) {
                            viewModel.sendMessage(inputText, executor)
                            inputText = ""
                        }
                    },
                    enabled = viewModel.isConnected && inputText.isNotEmpty()
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isUser) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
