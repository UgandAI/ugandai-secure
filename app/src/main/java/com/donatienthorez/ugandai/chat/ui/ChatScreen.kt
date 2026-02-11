package com.ugandai.ugandai.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.ugandai.ugandai.R
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.ugandai.ugandai.utils.HorizontalSpacer
import com.ugandai.ugandai.utils.VerticalSpacer
import kotlinx.coroutines.launch

data class ChatScreenUiHandlers(
    val onSendMessage: (String) -> Unit = {},
    val onResendMessage: (Message) -> Unit = {},
    val onAddToLogBook: (Message) -> Unit = {},   // ✅ Added
    val onNavigateToLogBook: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiHandlers: ChatScreenUiHandlers = ChatScreenUiHandlers(),
    conversation: LiveData<Conversation>,
    isSendingMessage: LiveData<Boolean>
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var inputValue by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val conversationState by conversation.observeAsState()
    val isSendingMessageState by isSendingMessage.observeAsState()

    fun sendMessage() {
        uiHandlers.onSendMessage(inputValue)
        inputValue = ""
        coroutineScope.launch {
            listState.animateScrollToItem(conversationState?.list?.size ?: 0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = { uiHandlers.onNavigateToLogBook() }) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Log Book"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues = paddingValues)
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                conversationState?.let {
                    MessageList(
                        messagesList = it.list,
                        listState = listState,
                        onResendMessage = uiHandlers.onResendMessage,
                        onAddToLogBook = uiHandlers.onAddToLogBook   // ✅ Added
                    )
                }
            }

            Row {
                TextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions { sendMessage() },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )

                HorizontalSpacer(8.dp)

                Button(
                    modifier = Modifier.height(56.dp),
                    onClick = { sendMessage() },
                    enabled = inputValue.isNotBlank() && isSendingMessageState != true,
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

@Composable
fun MessageList(
    messagesList: List<Message>,
    listState: LazyListState,
    onResendMessage: (Message) -> Unit,
    onAddToLogBook: (Message) -> Unit   // ✅ Added
) {
    LazyColumn(state = listState) {
        items(messagesList) { message ->

            Row(modifier = Modifier.fillMaxWidth()) {

                if (message.isFromUser) {
                    HorizontalSpacer(16.dp)
                    Box(modifier = Modifier.weight(1f))
                }

                SelectionContainer {
                    Text(
                        text = removeMarkdownMarkers(message.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        modifier = Modifier
                            .weight(2f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    message.messageStatus == MessageStatus.Error ->
                                        MaterialTheme.colorScheme.errorContainer
                                    message.isFromUser ->
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else ->
                                        MaterialTheme.colorScheme.primaryContainer
                                }
                            )
                            .clickable(enabled = message.messageStatus == MessageStatus.Error) {
                                onResendMessage(message)
                            }
                            .padding(8.dp)
                    )
                }

                if (!message.isFromUser) {
                    HorizontalSpacer(16.dp)
                    Box(modifier = Modifier.weight(1f))
                }
            }

            if (message.messageStatus == MessageStatus.Sending) {
                Text(
                    text = stringResource(R.string.chat_message_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 🔥 ADD TO LOGBOOK BUTTON
            if (!message.isFromUser && message.proposedActivity != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onAddToLogBook(message) }   // ✅ Modified
                ) {
                    Text(
                        text = "➕ Add to Logbook",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            VerticalSpacer(8.dp)
        }
    }
}

fun removeMarkdownMarkers(text: String): String {
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    val italicRegex = "\\*(.*?)\\*".toRegex()

    return text
        .replace(boldRegex, "$1")
        .replace(italicRegex, "$1")
}