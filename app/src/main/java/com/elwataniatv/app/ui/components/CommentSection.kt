package com.elwataniatv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elwataniatv.app.R
import com.elwataniatv.app.data.local.CommentEntity
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

@Composable
fun CommentSection(
    comments: List<CommentEntity>,
    onAddComment: (author: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var authorInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPanel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.comment_reactions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.comments_read_only),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )

        // Input Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = authorInput,
                onValueChange = { authorInput = it },
                placeholder = { Text(stringResource(R.string.comment_name_optional), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("comment_author_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text(stringResource(R.string.comment_write_hint), color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_text_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAddComment(authorInput, textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .testTag("submit_comment_btn")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_comment),
                        tint = Color.White
                    )
                }
            }
        }

        // List of Comments
        if (comments.isEmpty()) {
            Text(
                text = stringResource(R.string.be_first_comment),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                comments.take(15).forEach { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = comment.authorName,
                                color = BrandAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = comment.content,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        if (comment.likesCount > 0) {
                            Text(
                                text = stringResource(R.string.likes_count, comment.likesCount),
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
