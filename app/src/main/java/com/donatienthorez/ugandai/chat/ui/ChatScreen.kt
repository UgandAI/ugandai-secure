package com.ugandai.ugandai.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.donatienthorez.ugandai.chat.voice.VoiceModePhase
import com.donatienthorez.ugandai.chat.voice.VoiceModeState
import com.ugandai.ugandai.R
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.ugandai.ugandai.chat.data.ConversationSummary
import com.ugandai.ugandai.utils.HorizontalSpacer
import com.ugandai.ugandai.utils.VerticalSpacer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatScreenUiHandlers(
    val onSendMessage: (String) -> Unit = {},
    val onResendMessage: (Message) -> Unit = {},
    val onAddToLogBook: (Message) -> Unit = {},   // ✅ Added
    val onNavigateToLogBook: () -> Unit = {},
    val onNewConversation: () -> Unit = {},
    val onSelectConversation: (Int) -> Unit = {},
    val onEnterVoiceMode: () -> Unit = {},
    val onExitVoiceMode: () -> Unit = {},
    val onFinishVoiceTurn: () -> Unit = {},
    val onInterruptVoiceMode: () -> Unit = {},
    val onRetryVoiceMode: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiHandlers: ChatScreenUiHandlers = ChatScreenUiHandlers(),
    conversation: LiveData<Conversation>,
    isSendingMessage: LiveData<Boolean>,
    conversationSummaries: LiveData<List<ConversationSummary>>,
    selectedConversationId: LiveData<Int?>,
    voiceModeState: StateFlow<VoiceModeState>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var inputValue by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val conversationState by conversation.observeAsState()
    val isSendingMessageState by isSendingMessage.observeAsState()
    val summaries by conversationSummaries.observeAsState(emptyList())
    val selectedId by selectedConversationId.observeAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val voiceState by voiceModeState.collectAsState()

    val context = LocalContext.current

    fun sendMessage() {
        uiHandlers.onSendMessage(inputValue)
        inputValue = ""
        coroutineScope.launch {
            listState.animateScrollToItem(conversationState?.list?.size ?: 0)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) uiHandlers.onEnterVoiceMode()
        else coroutineScope.launch { snackbarHostState.showSnackbar("Microphone permission is required for Voice Mode") }
    }

    fun openVoiceMode() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            uiHandlers.onEnterVoiceMode()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Conversations", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
                NavigationDrawerItem(
                    label = { Text("New chat") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        uiHandlers.onNewConversation()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn {
                    items(summaries, key = { it.id }) { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            selected = item.id == selectedId,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                uiHandlers.onSelectConversation(item.id)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues = paddingValues)
        ) {
            // Top header with LogBook icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summaries.firstOrNull { it.id == selectedId }?.title ?: "Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.History, contentDescription = "Chat history", tint = Color(0xFF446F5D))
                    }
                    IconButton(onClick = uiHandlers.onNewConversation) {
                        Icon(Icons.Default.Add, contentDescription = "New chat", tint = Color(0xFF446F5D))
                    }
                    IconButton(onClick = { uiHandlers.onNavigateToLogBook() }) {
                        Icon(Icons.Default.Book, contentDescription = "Log Book", tint = Color(0xFF446F5D))
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                conversationState?.let {
                    MessageList(
                        messagesList = it.list,
                        listState = listState,
                        onResendMessage = uiHandlers.onResendMessage,
                        onAddToLogBook = uiHandlers.onAddToLogBook   // ✅ Added
                    )
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Button(
                    modifier = Modifier.height(56.dp),
                    onClick = { openVoiceMode() },
                    enabled = isSendingMessageState != true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF446F5D),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCCCCCC),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Open Voice Mode")
                }

                HorizontalSpacer(8.dp)

                TextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions { sendMessage() },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF1E1E1E),
                        unfocusedTextColor = Color(0xFF1E1E1E),
                        cursorColor = Color(0xFF446F5D),
                        focusedPlaceholderColor = Color(0xFF999999),
                        unfocusedPlaceholderColor = Color(0xFF999999)
                    ),
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            color = Color(0xFF999999)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalSpacer(8.dp)

                Button(
                    modifier = Modifier.height(56.dp),
                    onClick = { sendMessage() },
                    enabled = inputValue.isNotBlank() && isSendingMessageState != true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF446F5D),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCCCCCC),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSendingMessageState == true) {
                        Icon(Icons.Default.Sync, contentDescription = "Sending")
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
    }

    if (voiceState.phase != VoiceModePhase.Idle) {
        VoiceModeDialog(
            state = voiceState,
            onClose = uiHandlers.onExitVoiceMode,
            onFinishTurn = uiHandlers.onFinishVoiceTurn,
            onInterrupt = uiHandlers.onInterruptVoiceMode,
            onRetry = uiHandlers.onRetryVoiceMode
        )
    }
}

@Composable
private fun VoiceModeDialog(
    state: VoiceModeState,
    onClose: () -> Unit,
    onFinishTurn: () -> Unit,
    onInterrupt: () -> Unit,
    onRetry: () -> Unit
) {
    BackHandler(onBack = onClose)
    val responseScrollState = rememberScrollState()
    var previousAssistantResponse by remember { mutableStateOf("") }
    val preview = when {
        state.phase == VoiceModePhase.Error -> state.errorMessage.orEmpty()
        state.assistantText.isNotBlank() -> state.assistantText
        else -> state.partialTranscript
    }
    val isAssistantResponse = state.assistantText.isNotBlank()

    LaunchedEffect(preview, isAssistantResponse) {
        if (isAssistantResponse && preview != previousAssistantResponse) {
            val newResponse = previousAssistantResponse.isBlank()
            val wasNearBottom = responseScrollState.maxValue - responseScrollState.value <= 96
            if (newResponse) {
                responseScrollState.scrollTo(0)
            } else if (wasNearBottom) {
                responseScrollState.scrollTo(responseScrollState.maxValue)
            }
        }
        previousAssistantResponse = if (isAssistantResponse) preview else ""
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color(0xFFF3F7F4), modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Voice Mode",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Exit Voice Mode") }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White
                ) {
                    if (preview.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Your conversation will appear here.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF667069)
                            )
                        }
                    } else {
                        SelectionContainer {
                            Text(
                                text = formatVoiceResponse(preview),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(responseScrollState)
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                                color = Color(0xFF26312B)
                            )
                        }
                    }
                }

                val activeColor = when (state.phase) {
                    VoiceModePhase.Error -> Color(0xFFD32F2F)
                    VoiceModePhase.AssistantSpeaking -> Color(0xFFDA8B3C)
                    VoiceModePhase.UserSpeaking -> Color(0xFF2E7D5B)
                    else -> Color(0xFF446F5D)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(100.dp),
                        color = activeColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, activeColor)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    HorizontalSpacer(10.dp)
                    Text(
                        text = when (state.phase) {
                            VoiceModePhase.Initializing -> "Getting ready…"
                            VoiceModePhase.Listening, VoiceModePhase.UserSpeaking -> "Listening…"
                            VoiceModePhase.Transcribing -> "Got it…"
                            VoiceModePhase.Thinking -> "Thinking…"
                            VoiceModePhase.AssistantSpeaking -> "Speaking…"
                            VoiceModePhase.Error -> "Try again"
                            VoiceModePhase.Idle -> ""
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    when (state.phase) {
                        VoiceModePhase.Listening, VoiceModePhase.UserSpeaking -> Button(onClick = onFinishTurn) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            HorizontalSpacer(6.dp)
                            Text("Send now")
                        }
                        VoiceModePhase.AssistantSpeaking -> Button(onClick = onInterrupt) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            HorizontalSpacer(6.dp)
                            Text("Interrupt")
                        }
                        VoiceModePhase.Error -> Button(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            HorizontalSpacer(6.dp)
                            Text("Try again")
                        }
                        else -> Unit
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                    Text("End Voice Mode")
                }
            }
        }
    }
}

private fun formatVoiceResponse(text: String): AnnotatedString = buildAnnotatedString {
    val lines = removeLatexMarkers(text).lines()
    lines.forEachIndexed { index, rawLine ->
        val heading = rawLine.trimStart().startsWith("#")
        val unheaded = if (heading) rawLine.trimStart().trimStart('#').trimStart() else rawLine
        val line = when {
            unheaded.startsWith("- ") -> "• ${unheaded.drop(2)}"
            unheaded.startsWith("* ") -> "• ${unheaded.drop(2)}"
            else -> unheaded
        }
        if (heading) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendVoiceBoldSpans(line) }
        } else {
            appendVoiceBoldSpans(line)
        }
        if (index != lines.lastIndex) append('\n')
    }
}

private fun AnnotatedString.Builder.appendVoiceBoldSpans(line: String) {
    var cursor = 0
    Regex("\\*\\*(.+?)\\*\\*").findAll(line).forEach { match ->
        append(line.substring(cursor, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        cursor = match.range.last + 1
    }
    append(line.substring(cursor))
}

@Composable
fun MessageList(
    messagesList: List<Message>,
    listState: LazyListState,
    onResendMessage: (Message) -> Unit,
    onAddToLogBook: (Message) -> Unit   // ✅ Added
) {
    LazyColumn(state = listState) {
        items(messagesList) { message ->

            val isPendingAssistantReply = !message.isFromUser &&
                message.messageStatus == MessageStatus.Sending &&
                message.text.isBlank()

            if (!isPendingAssistantReply) {
                Row(modifier = Modifier.fillMaxWidth()) {

                    if (message.isFromUser) {
                        HorizontalSpacer(16.dp)
                        Box(modifier = Modifier.weight(1f))
                    }

                    SelectionContainer {
                        Surface(
                            modifier = Modifier.weight(2f, fill = false),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                message.messageStatus == MessageStatus.Error ->
                                    Color(0xFFFFCDD2)
                                message.isFromUser ->
                                    Color(0xFF446F5D)
                                else ->
                                    Color.White
                            },
                            shadowElevation = 4.dp,
                            border = if (!message.isFromUser && message.messageStatus != MessageStatus.Error) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                            } else null
                        ) {
                            Text(
                                text = removeMarkdownMarkers(message.text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (message.isFromUser) Color.White else Color(0xFF1E1E1E),
                                modifier = Modifier
                                    .clickable(enabled = message.messageStatus == MessageStatus.Error) {
                                        onResendMessage(message)
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }

                    if (!message.isFromUser) {
                        HorizontalSpacer(16.dp)
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (isPendingAssistantReply) {
                Text(
                    text = stringResource(R.string.chat_message_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                HorizontalSpacer(32.dp)
            }

            if (message.messageStatus == MessageStatus.Error) {
                Row(
                    modifier = Modifier.clickable {
                        onResendMessage(message)
                    }
                ) {
                    Box(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.chat_message_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            // 🔥 ADD TO LOGBOOK BUTTON
            if (!message.isFromUser && message.proposedActivity != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF446F5D))
                        .clickable { onAddToLogBook(message) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "➕ Add to Logbook",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!message.isFromUser && message.citations.isNotEmpty()) {
                var sourcesExpanded by remember(message.id) { mutableStateOf(false) }
                Column(modifier = Modifier.padding(top = 4.dp, start = 12.dp)) {
                    Text(
                        text = if (sourcesExpanded) "Hide sources" else "Sources (${message.citations.size})",
                        color = Color(0xFF446F5D),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { sourcesExpanded = !sourcesExpanded }
                    )
                    if (sourcesExpanded) {
                        message.citations.forEach { citation ->
                            Text(
                                text = "• ${citation.title}${citation.url?.let { " — $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            VerticalSpacer(8.dp)
        }
    }
}

fun removeMarkdownMarkers(text: String): String {
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    val italicRegex = "\\*(.*?)\\*".toRegex()

    return removeLatexMarkers(text)
        .replace(boldRegex, "$1")
        .replace(italicRegex, "$1")
}

/**
 * The chat bubble renders plain text, not LaTeX, but the model sometimes replies with
 * math markup anyway (e.g. `\[25,000 \text{ plants/acre}\]`). Strip the delimiters/commands
 * down to readable plain text instead of showing raw LaTeX source.
 */
fun removeLatexMarkers(text: String): String {
    val textCommandRegex = "\\\\text\\{(.*?)\\}".toRegex()
    val fracRegex = "\\\\frac\\{(.*?)\\}\\{(.*?)\\}".toRegex()

    return text
        .replace(fracRegex, "$1/$2")
        .replace(textCommandRegex, "$1")
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\cdot", "·")
        .replace("\\approx", "≈")
        .replace("\\[", "")
        .replace("\\]", "")
        .replace("\\(", "")
        .replace("\\)", "")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
