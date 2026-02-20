package com.ugandai.ugandai.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                    text = "Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
                IconButton(
                    onClick = { uiHandlers.onNavigateToLogBook() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Log Book",
                        tint = Color(0xFF446F5D)
                    )
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
                        textColor = Color(0xFF1E1E1E),
                        cursorColor = Color(0xFF446F5D),
                        placeholderColor = Color(0xFF999999)
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

            if (message.messageStatus == MessageStatus.Sending) {
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