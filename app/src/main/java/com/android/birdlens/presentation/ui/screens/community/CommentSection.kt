// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CommentSection.kt
package com.android.birdlens.presentation.ui.screens.community

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.presentation.viewmodel.CommunityViewModel
import com.android.birdlens.presentation.viewmodel.GenericUiState
import com.android.birdlens.ui.theme.CardBackground
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatTimestamp(isoTimestamp: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoTimestamp)
        val localDateTime = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        localDateTime.format(formatter)
    } catch (e: Exception) {
        Log.w("FormatTimestamp", "Failed to parse timestamp '$isoTimestamp': ${e.message}")
        isoTimestamp
    }
}

@Composable
fun CommentSection(
    postId: String,
    communityViewModel: CommunityViewModel,
    onDismiss: () -> Unit
) {
    val commentsUiState by communityViewModel.commentsState.collectAsState()
    var newCommentText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 300.dp)
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Comments", style = MaterialTheme.typography.titleLarge, color = TextWhite)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close comments", tint = TextWhite)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (val state = commentsUiState) {
            is GenericUiState.Loading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextWhite)
                }
            }
            is GenericUiState.Success -> {
                val items = state.data.items // Get items, which might be null
                if (items.isNullOrEmpty()) { // Check if null or empty
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "No comments yet. Be the first to comment!",
                            color = TextWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(items, key = { it.id }) { comment -> // `items` is now guaranteed non-null here
                            CommentItem(comment)
                        }
                    }
                }
            }
            is GenericUiState.Error -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = Color.Red, textAlign = TextAlign.Center)
                }
            }
            is GenericUiState.Idle -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Loading comments...", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Add a comment...", color = TextWhite.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = TextWhite,
                    focusedIndicatorColor = GreenWave2,
                    unfocusedIndicatorColor = TextWhite.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        communityViewModel.createCommentForPost(postId, newCommentText)
                        newCommentText = ""
                    } else {
                        Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = newCommentText.isNotBlank() && commentsUiState !is GenericUiState.Loading
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post Comment", tint = GreenWave2)
            }
        }
    }
}

@Composable
fun CommentItem(comment: com.android.birdlens.data.model.post.CommentResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = comment.userAvatarUrl,
                error = painterResource(id = R.drawable.ic_launcher_foreground),
                placeholder = painterResource(id = R.drawable.ic_launcher_background)
            ),
            contentDescription = "Avatar of ${comment.userFullName}",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = comment.userFullName,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.content,
                color = TextWhite.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatTimestamp(comment.createdAt),
                color = TextWhite.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}