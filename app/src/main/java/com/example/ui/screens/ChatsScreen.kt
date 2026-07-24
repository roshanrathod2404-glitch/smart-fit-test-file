package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ContactEntity
import com.example.data.HistoryEntity
import com.example.data.MessageEntity
import com.example.ui.theme.BgCharcoal
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSilver
import com.example.ui.theme.TextWhite
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.Flow
import android.media.MediaRecorder
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.net.Uri
import androidx.compose.ui.text.style.TextOverflow
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.gestures.detectVerticalDragGestures

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(
    contact: ContactEntity?,
    messages: List<MessageEntity>,
    contacts: List<ContactEntity>,
    onBack: () -> Unit,
    onSendMessage: (String, String, String, String, String) -> Unit,
    onSendVoiceMessage: ((String, String, String, String, String) -> Unit)? = null,
    onToggleStar: (Long, Boolean) -> Unit,
    onClearChat: (Long) -> Unit,
    onDeleteOldMessages: (Long, Long) -> Unit,
    onGetHistoryForContact: (Long, String) -> Flow<List<HistoryEntity>>,
    onGetStarredMessages: (Long) -> Flow<List<MessageEntity>>,
    onBlockContact: (Long) -> Unit,
    onCreateGroup: (String, Set<Long>, (ContactEntity) -> Unit) -> Unit,
    onUpdateLocalOverrideAvatar: (Long, String) -> Unit,
    typingStatusMap: Map<Long, Boolean>,
    onUpdateTypingStatus: (Long, Boolean) -> Unit,
    onDeleteMessageForMe: ((Long) -> Unit)? = null,
    onDeleteMessageForEveryone: ((Long) -> Unit)? = null,
    onToggleMessageStarredByRecipient: ((Long, Boolean) -> Unit)? = null,
    deletionOutcomeFlow: kotlinx.coroutines.flow.SharedFlow<String>? = null,
    currentLang: String = "English"
) {
    val context = LocalContext.current
    var playingMessageId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var currentPlaybackPosition by remember { mutableStateOf(0f) }
    var playbackDuration by remember { mutableStateOf(100f) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(playingMessageId) {
        if (playingMessageId != null) {
            while (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                try {
                    currentPlaybackPosition = mediaPlayer!!.currentPosition.toFloat()
                    playbackDuration = mediaPlayer!!.duration.coerceAtLeast(1).toFloat()
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(100)
            }
        } else {
            currentPlaybackPosition = 0f
        }
    }
    var textInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var replyingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showChatInfo by remember { mutableStateOf(false) }
    
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isSheetExpanded by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showFullScreenCamera by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showFullScreenCamera = true
            showAttachmentMenu = false
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "📄 Document Shared: $uri",
                replyingMessage?.messageText ?: "",
                replyingMessage?.senderName ?: "",
                "Document",
                uri.toString()
            )
            replyingMessage = null
            showAttachmentMenu = false
            Toast.makeText(context, "Document shared", Toast.LENGTH_SHORT).show()
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "📑 PDF Shared: $uri",
                replyingMessage?.messageText ?: "",
                replyingMessage?.senderName ?: "",
                "PDF",
                uri.toString()
            )
            replyingMessage = null
            showAttachmentMenu = false
            Toast.makeText(context, "PDF shared", Toast.LENGTH_SHORT).show()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        onSendMessage(
            "👤 Contact Shared: Coach Alex (Fitness Expert - +1 555-0199)",
            replyingMessage?.messageText ?: "",
            replyingMessage?.senderName ?: "",
            "Text",
            ""
        )
        replyingMessage = null
        showAttachmentMenu = false
        Toast.makeText(context, "Contact shared", Toast.LENGTH_SHORT).show()
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSendMessage(
                "🎵 Audio Shared: $uri",
                replyingMessage?.messageText ?: "",
                replyingMessage?.senderName ?: "",
                "Document",
                uri.toString()
            )
            replyingMessage = null
            showAttachmentMenu = false
            Toast.makeText(context, "Audio shared", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(textInput) {
        if (textInput.isNotBlank()) {
            onUpdateTypingStatus(contact?.id ?: 0L, true)
            kotlinx.coroutines.delay(2000)
            onUpdateTypingStatus(contact?.id ?: 0L, false)
        } else {
            onUpdateTypingStatus(contact?.id ?: 0L, false)
        }
    }
    
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }
    
    var activeSubDialog by remember { mutableStateOf<String?>(null) } // "history_menu", "history_detail", "disappearing", "star_messages"
    var selectedHistoryCategory by remember { mutableStateOf<String?>(null) } // "Photo", "Video", "Document", "PDF"
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedMessageForStar by remember { mutableStateOf<MessageEntity?>(null) }
    var longPressedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showDpPreviewModal by remember { mutableStateOf(false) }

    if (contact == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCharcoal),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a contact from Home to start messaging", color = TextSilver, fontSize = 14.sp)
        }
        return
    }

    if (showDpPreviewModal) {
        ProfilePicturePreviewDialog(
            contactName = contact.name,
            avatarUrl = contact.localOverrideAvatar,
            avatarInitials = contact.avatarInitials,
            currentLang = currentLang,
            onDismiss = { showDpPreviewModal = false },
            onInfoClick = {
                showDpPreviewModal = false
                showChatInfo = true
            }
        )
    }

    if (showChatInfo) {
        ChatInfoScreen(
            contact = contact,
            messages = messages,
            contacts = contacts,
            onBack = { showChatInfo = false },
            onClearChat = onClearChat,
            onBlockContact = { contactId ->
                onBlockContact(contactId)
                showChatInfo = false
                onBack()
            },
            onDeleteOldMessages = onDeleteOldMessages,
            onCreateGroup = onCreateGroup,
            onUpdateLocalOverrideAvatar = onUpdateLocalOverrideAvatar
        )
        return
    }

    val displayedMessages = if (isSearchActive && searchQuery.isNotBlank()) {
        messages.filter { it.messageText.contains(searchQuery, ignoreCase = true) }
    } else {
        messages
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCharcoal)
            .imePadding()
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BgCharcoal)
                        .clickable { showDpPreviewModal = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (contact.localOverrideAvatar.isNotBlank()) {
                        AsyncImage(
                            model = contact.localOverrideAvatar,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = contact.avatarInitials, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showChatInfo = true }
                ) {
                    Text(text = contact.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val isTyping = typingStatusMap[contact.id] == true
                    val subText = if (isTyping) {
                        Strings.get("typing", currentLang)
                    } else {
                        when (contact.onlineStatus) {
                            "Online" -> Strings.get("online", currentLang)
                            "Offline" -> Strings.get("offline", currentLang)
                            else -> {
                                if (contact.onlineStatus.startsWith("Last seen", ignoreCase = true)) {
                                    val rest = contact.onlineStatus.substring("Last seen".length)
                                    Strings.get("last_seen", currentLang) + rest
                                } else {
                                    contact.onlineStatus
                                }
                            }
                        }
                    }
                    Text(
                        text = subText,
                        color = if (isTyping) Color(0xFF00FF66) else TextSilver,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextWhite)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    // Exact order: History, Search, Mute Notifications, Clear Chat, Block Contact, Disappearing Messages, Star Messages
                    DropdownMenuItem(
                        text = { Text("History", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            activeSubDialog = "history_menu"
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Search", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            isSearchActive = true
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Mute Notifications", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "Notifications muted for ${contact.name}", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Chat", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            onClearChat(contact.id)
                            Toast.makeText(context, "Chat cleared (Starred messages protected)", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Block Contact", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "${contact.name} blocked", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Disappearing Messages", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            activeSubDialog = "disappearing"
                        },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = TextWhite) }
                    )
                    DropdownMenuItem(
                        text = { Text("Star Messages", color = TextWhite) },
                        onClick = {
                            showMenu = false
                            activeSubDialog = "star_messages"
                        },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = TextWhite) }
                    )
                }
            }
        }

        // Search Bar if active
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.9f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search in chat...", color = TextSilver) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = TextWhite,
                        unfocusedBorderColor = TextSilver
                    ),
                    singleLine = true
                )
                IconButton(onClick = {
                    isSearchActive = false
                    searchQuery = ""
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close Search", tint = TextWhite)
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(displayedMessages) { message ->
                val isMe = message.isSentByMe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 30f) {
                                    replyingMessage = message
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                longPressedMessage = message
                            }
                        ),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                color = if (isMe) Color.White.copy(alpha = 0.15f) else SurfaceDark,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            if (message.replyToText.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text(text = message.replyToSender, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(text = message.replyToText, color = TextSilver, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            if (message.fileType == "Voice" || message.messageText.startsWith("🎙️")) {
                                val isPlayingThis = playingMessageId == message.id
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingThis) {
                                                mediaPlayer?.pause()
                                                playingMessageId = null
                                            } else {
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                try {
                                                    val filePath = message.fileName
                                                    val file = if (filePath.isNotBlank() && File(filePath).exists()) File(filePath) else null
                                                    val mp = if (file != null) {
                                                        android.media.MediaPlayer.create(context, android.net.Uri.fromFile(file))
                                                    } else {
                                                        val dummy = File(context.cacheDir, "dummy_voice.aac")
                                                        if (!dummy.exists()) dummy.writeBytes(ByteArray(50))
                                                        android.media.MediaPlayer.create(context, android.net.Uri.fromFile(dummy))
                                                    }
                                                    mp?.setOnCompletionListener {
                                                        playingMessageId = null
                                                        currentPlaybackPosition = 0f
                                                        it.release()
                                                        mediaPlayer = null
                                                    }
                                                    mp?.start()
                                                    mediaPlayer = mp
                                                    playingMessageId = message.id
                                                    playbackDuration = mp?.duration?.coerceAtLeast(1)?.toFloat() ?: 100f
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingThis) "Pause" else "Play",
                                            tint = TextWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = if (isPlayingThis) currentPlaybackPosition else 0f,
                                        onValueChange = { newVal ->
                                            currentPlaybackPosition = newVal
                                            mediaPlayer?.seekTo(newVal.toInt())
                                        },
                                        valueRange = 0f..playbackDuration,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = message.messageText,
                                        color = TextWhite,
                                        fontSize = 12.sp
                                    )
                                    if (message.isStarred) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Starred",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else if (message.fileType == "Photo" || message.messageText.contains("Photo Shared:") || message.messageText.contains("Media Attachment:")) {
                                val photoUri = message.fileName.ifBlank {
                                    val text = message.messageText
                                    if (text.contains("Photo Shared: ")) {
                                        text.substringAfter("Photo Shared: ").trim()
                                    } else if (text.contains("Media Attachment: ")) {
                                        text.substringAfter("Media Attachment: ").trim()
                                    } else {
                                        text
                                    }
                                }
                                val isMockMedia = !photoUri.startsWith("content://") && !photoUri.startsWith("file://")
                                val imageModel: Any = if (isMockMedia) {
                                    "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400&auto=format&fit=crop"
                                } else {
                                    photoUri
                                }

                                Column {
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = "Shared Photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                fullScreenImageUri = photoUri
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    if (message.messageText.isNotBlank() && !message.messageText.startsWith("📷") && !message.messageText.startsWith("🖼️")) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = message.messageText,
                                            color = TextWhite,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else if (message.fileType == "Video" || message.messageText.contains("Video Recorded:") || message.messageText.startsWith("🎥")) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable {
                                            Toast.makeText(context, "Playing video...", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1518310383802-640c2de311b2?w=400&auto=format&fit=crop",
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.4f
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Text(
                                        text = "Video Preview",
                                        color = TextWhite,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                                    )
                                }
                            } else if (message.fileType == "Document" || message.fileType == "PDF" || message.messageText.contains("Document Shared:") || message.messageText.contains("PDF Shared:") || message.messageText.startsWith("📄") || message.messageText.startsWith("📑")) {
                                val fileUri = message.fileName.ifBlank {
                                    val text = message.messageText
                                    if (text.contains("Document Shared: ")) text.substringAfter("Document Shared: ").trim()
                                    else if (text.contains("PDF Shared: ")) text.substringAfter("PDF Shared: ").trim()
                                    else text
                                }
                                val docName = if (fileUri.contains("/")) fileUri.substringAfterLast("/") else "WorkoutPlan.pdf"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "File Icon",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = docName,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Shared Document",
                                            color = TextSilver,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Opening document...", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = TextWhite, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message.messageText,
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (message.isStarred) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Starred",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.timestamp,
                                color = TextSilver,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Reply Preview Box if active
        if (replyingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Replying to ${replyingMessage!!.senderName}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = replyingMessage!!.messageText, color = TextSilver, fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = { replyingMessage = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = TextWhite)
                }
            }
        }

        // Input bar
        if (contact?.isAdminOnlyPosting == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Only community admins can send messages",
                        color = TextSilver,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            IconButton(
                onClick = { showAttachmentMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TextSilver)
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onUpdateTypingStatus(contact.id, true)
                },
                placeholder = { Text("Type a message...", color = TextSilver) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextWhite,
                    unfocusedBorderColor = TextSilver.copy(alpha = 0.4f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isRecording) Color.Red else SurfaceDark, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isRecording = true
                                val audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.aac")
                                try {
                                    @Suppress("DEPRECATION")
                                    val rec = MediaRecorder()
                                    rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                                    rec.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                                    rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    rec.setOutputFile(audioFile.absolutePath)
                                    rec.prepare()
                                    rec.start()
                                    mediaRecorder = rec
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                try {
                                    tryAwaitRelease()
                                } catch (e: Exception) {}

                                isRecording = false
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                    mediaRecorder = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                onSendMessage(
                                    "🎙️ Voice Note (0:04)",
                                    replyingMessage?.messageText ?: "",
                                    replyingMessage?.senderName ?: "",
                                    "Voice",
                                    ""
                                )
                                replyingMessage = null
                                Toast.makeText(context, "Voice note sent successfully", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Message",
                    tint = if (isRecording) TextWhite else TextSilver
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(
                            textInput,
                            replyingMessage?.messageText ?: "",
                            replyingMessage?.senderName ?: "",
                            "Text",
                            ""
                        )
                        textInput = ""
                        replyingMessage = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(TextWhite, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BgCharcoal)
            }
        }
        }
    }

    // Dialogs / Sub-screens
    // Observes backend/socket deletion events
    if (deletionOutcomeFlow != null) {
        LaunchedEffect(Unit) {
            deletionOutcomeFlow.collect { outcome ->
                Toast.makeText(context, outcome, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Dialogs / Sub-screens
    if (longPressedMessage != null) {
        val msg = longPressedMessage!!
        AlertDialog(
            onDismissRequest = { longPressedMessage = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = Strings.get("message_options", currentLang),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Preview of the message text or file type
                    Text(
                        text = when {
                            msg.fileType == "Voice" -> "🎙️ " + Strings.get("voice_note", currentLang)
                            msg.fileType == "Photo" -> "📷 " + Strings.get("shared_photo", currentLang)
                            msg.fileType == "Video" -> "🎥 " + Strings.get("video_preview", currentLang)
                            msg.fileType == "Document" -> "📄 " + Strings.get("document", currentLang)
                            msg.fileType == "PDF" -> "📑 " + Strings.get("pdf_document", currentLang)
                            else -> msg.messageText
                        },
                        color = TextSilver,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BgCharcoal, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Star / Unstar Option
                    Button(
                        onClick = {
                            onToggleStar(msg.id, !msg.isStarred)
                            longPressedMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (msg.isStarred) Color(0xFFFFD700) else TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (msg.isStarred) Strings.get("unstar_message", currentLang) else Strings.get("star_message", currentLang),
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 2. Delete for Me Option
                    Button(
                        onClick = {
                            onDeleteMessageForMe?.invoke(msg.id)
                            longPressedMessage = null
                            Toast.makeText(context, "Message deleted for me", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Strings.get("delete_for_me", currentLang),
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 3. Delete for Everyone Option
                    Button(
                        onClick = {
                            onDeleteMessageForEveryone?.invoke(msg.id)
                            longPressedMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Strings.get("delete_for_everyone", currentLang),
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 4. Simulate Recipient Star (Only if sent by me)
                    if (msg.isSentByMe && onToggleMessageStarredByRecipient != null) {
                        val isRecStar = msg.isStarredByRecipient
                        Button(
                            onClick = {
                                onToggleMessageStarredByRecipient(msg.id, !isRecStar)
                                // We update local object so state is fresh in UI without re-long-pressing
                                longPressedMessage = msg.copy(isStarredByRecipient = !isRecStar)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isRecStar) Color(0xFFFFD700) else TextSilver,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isRecStar) {
                                        Strings.get("recipient_starred_yes", currentLang)
                                    } else {
                                        Strings.get("recipient_starred_no", currentLang)
                                    },
                                    color = TextWhite,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressedMessage = null }) {
                    Text("Close", color = TextSilver, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (selectedMessageForStar != null) {
        val msg = selectedMessageForStar!!
        AlertDialog(
            onDismissRequest = { selectedMessageForStar = null },
            containerColor = SurfaceDark,
            title = { Text(if (msg.isStarred) "Unstar Message?" else "Star Message?", color = TextWhite) },
            text = { Text(msg.messageText, color = TextSilver) },
            confirmButton = {
                TextButton(onClick = {
                    onToggleStar(msg.id, !msg.isStarred)
                    selectedMessageForStar = null
                }) {
                    Text(if (msg.isStarred) "Unstar It" else "Star It", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessageForStar = null }) {
                    Text("Cancel", color = TextSilver)
                }
            }
        )
    }

    if (activeSubDialog == "history_menu") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            containerColor = SurfaceDark,
            title = { Text("Contact Media History", color = TextWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("Photo", "Video", "Document", "PDF")
                    categories.forEach { cat ->
                        Button(
                            onClick = {
                                selectedHistoryCategory = cat
                                activeSubDialog = "history_detail"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$cat History", color = TextWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) {
                    Text("Close", color = TextSilver)
                }
            }
        )
    }

    if (activeSubDialog == "history_detail" && selectedHistoryCategory != null) {
        val historyList by onGetHistoryForContact(contact.id, selectedHistoryCategory!!).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { activeSubDialog = "history_menu"; selectedHistoryCategory = null },
            containerColor = SurfaceDark,
            title = { Text("$selectedHistoryCategory History for ${contact.name}", color = TextWhite) },
            text = {
                if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No $selectedHistoryCategory records found for this contact.", color = TextSilver, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgCharcoal, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(text = item.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = item.subtitle, color = TextSilver, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Sender: ${item.senderName}", color = TextWhite.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text(text = "${item.dateText} | ${item.timeText}", color = TextSilver, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = "history_menu"; selectedHistoryCategory = null }) {
                    Text("Back", color = TextWhite)
                }
            }
        )
    }

    if (activeSubDialog == "disappearing") {
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            containerColor = SurfaceDark,
            title = { Text("Disappearing Messages", color = TextWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select timer to automatically delete messages older than duration (Starred messages remain protected):", color = TextSilver, fontSize = 13.sp)
                    Button(
                        onClick = {
                            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                            onDeleteOldMessages(contact.id, cutoff)
                            activeSubDialog = null
                            Toast.makeText(context, "Disappearing (24h) applied. Starred protected.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("24 Hours", color = TextWhite) }

                    Button(
                        onClick = {
                            val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
                            onDeleteOldMessages(contact.id, cutoff)
                            activeSubDialog = null
                            Toast.makeText(context, "Disappearing (7 Days) applied. Starred protected.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("7 Days", color = TextWhite) }

                    Button(
                        onClick = {
                            activeSubDialog = null
                            Toast.makeText(context, "Disappearing Messages turned Off", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BgCharcoal),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Off", color = TextWhite) }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) {
                    Text("Close", color = TextSilver)
                }
            }
        )
    }

    if (activeSubDialog == "star_messages") {
        val starredList by onGetStarredMessages(contact.id).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { activeSubDialog = null },
            containerColor = SurfaceDark,
            title = { Text("Starred Messages (${contact.name})", color = TextWhite) },
            text = {
                if (starredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No starred messages for this contact.", color = TextSilver, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(starredList) { msg ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgCharcoal, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(text = msg.messageText, color = TextWhite, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = msg.senderName, color = TextSilver, fontSize = 11.sp)
                                    Text(text = msg.timestamp, color = TextSilver, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeSubDialog = null }) {
                    Text("Close", color = TextWhite)
                }
            }
        )
    }

    if (showFullScreenCamera) {
        var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
        val previewView = remember {
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }
        val imageCapture = remember { ImageCapture.Builder().build() }
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(lensFacing, showFullScreenCamera) {
            kotlinx.coroutines.delay(120)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Top Row Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showFullScreenCamera = false },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White)
                }
                Text(
                    text = "Camera",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                IconButton(
                    onClick = { Toast.makeText(context, "Flash toggled", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = "Flash", tint = Color.White)
                }
            }

            // Bottom Row Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            val file = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val savedUri = Uri.fromFile(file).toString()
                                        onSendMessage(
                                            "📷 Photo Captured",
                                            replyingMessage?.messageText ?: "",
                                            replyingMessage?.senderName ?: "",
                                            "Photo",
                                            savedUri
                                        )
                                        replyingMessage = null
                                        showFullScreenCamera = false
                                        Toast.makeText(context, "Photo captured & sent", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Box(modifier = Modifier.size(48.dp))
            }
        }
    }

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    if (showAttachmentMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showAttachmentMenu = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (isSheetExpanded) it.fillMaxHeight(0.85f) else it.wrapContentHeight() }
                    .clickable(enabled = false) {}
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount < -15) {
                                    isSheetExpanded = true
                                } else if (dragAmount > 15) {
                                    isSheetExpanded = false
                                }
                            }
                        )
                    },
                color = SurfaceDark,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (isSheetExpanded) it.fillMaxHeight() else it.wrapContentHeight() }
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TextSilver.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                            .clickable { isSheetExpanded = !isSheetExpanded }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("share_content_media", currentLang),
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { isSheetExpanded = !isSheetExpanded }) {
                            Icon(
                                imageVector = if (isSheetExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = "Expand/Collapse",
                                tint = TextSilver
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val galleryItems = listOf(
                        "Camera Stub",
                        "Workout Photo 1",
                        "Gym Progress",
                        "Meal Prep",
                        "Cardio Session",
                        "Stretching",
                        "Yoga Routine",
                        "Running Session",
                        "Crossfit Training",
                        "Protein Shake",
                        "Dumbbell Curls",
                        "Treadmill Run",
                        "Cycling Session",
                        "Swimming Laps",
                        "HIIT Workout",
                        "Core Strength"
                    )

                    val gridModifier = if (isSheetExpanded) {
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    }

                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        modifier = gridModifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(galleryItems.size) { index ->
                            val item = galleryItems[index]
                            if (index == 0) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1A1A24))
                                        .clickable {
                                            if (hasCameraPermission) {
                                                showFullScreenCamera = true
                                                showAttachmentMenu = false
                                            } else {
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasCameraPermission && !showFullScreenCamera) {
                                        val inlinePreviewView = remember {
                                            PreviewView(context).apply {
                                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                            }
                                        }
                                        LaunchedEffect(inlinePreviewView, showFullScreenCamera) {
                                            if (!showFullScreenCamera) {
                                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                                cameraProviderFuture.addListener({
                                                    try {
                                                        val cameraProvider = cameraProviderFuture.get()
                                                        cameraProvider.unbindAll()
                                                        val preview = Preview.Builder().build().also {
                                                            it.setSurfaceProvider(inlinePreviewView.surfaceProvider)
                                                        }
                                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                                        cameraProvider.bindToLifecycle(
                                                            lifecycleOwner,
                                                            cameraSelector,
                                                            preview
                                                        )
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }, ContextCompat.getMainExecutor(context))
                                            }
                                        }

                                        DisposableEffect(showFullScreenCamera) {
                                            onDispose {
                                                if (!showFullScreenCamera) {
                                                    try {
                                                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                                                        cameraProvider.unbindAll()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }

                                        AndroidView(
                                            factory = { inlinePreviewView },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.BottomStart
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF3333))
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(Strings.get("live", currentLang), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color(0xFF00FF66), modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(Strings.get("camera", currentLang), color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BgCharcoal)
                                        .clickable {
                                            onSendMessage(
                                                "🖼️ Media Attachment: $item",
                                                replyingMessage?.messageText ?: "",
                                                replyingMessage?.senderName ?: "",
                                                "Photo",
                                                item
                                            )
                                            replyingMessage = null
                                            showAttachmentMenu = false
                                            Toast.makeText(context, "Sent $item", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = TextSilver, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item, color = TextWhite, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = TextSilver.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val attachmentOptions = listOf(
                        Triple("File", Icons.Default.InsertDriveFile, { documentPickerLauncher.launch("*/*") }),
                        Triple("PDF", Icons.Default.PictureAsPdf, { pdfPickerLauncher.launch("application/pdf") }),
                        Triple("Documents", Icons.Default.Description, { documentPickerLauncher.launch("application/msword") }),
                        Triple("Contact", Icons.Default.Person, { contactPickerLauncher.launch(null) }),
                        Triple("Music", Icons.Default.AudioFile, { audioPickerLauncher.launch("audio/*") })
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        attachmentOptions.forEach { (title, icon, action) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { action() }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2A2A3C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = title,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (fullScreenImageUri != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { fullScreenImageUri = null },
            contentAlignment = Alignment.Center
        ) {
            val isMockMedia = !fullScreenImageUri!!.startsWith("content://") && !fullScreenImageUri!!.startsWith("file://")
            val imageModel: Any = if (isMockMedia) {
                "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800&auto=format&fit=crop"
            } else {
                fullScreenImageUri!!
            }
            AsyncImage(
                model = imageModel,
                contentDescription = "Full Screen Photo",
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = { fullScreenImageUri = null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close View", tint = Color.White)
            }
        }
    }
}
