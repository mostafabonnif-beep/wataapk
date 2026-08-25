package com.elwataniatv.app.ui.components

import android.net.Uri
import android.widget.Toast
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.elwataniatv.app.R
import com.google.firebase.storage.FirebaseStorage
import com.elwataniatv.app.ui.theme.BrandAccent
import com.elwataniatv.app.ui.theme.BrandPanel
import com.elwataniatv.app.ui.theme.BrandPrimary

@Composable
fun FirebaseMediaUploader(
    currentUrl: String,
    label: String = "",
    folderPath: String = "",
    onUrlGenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var showUrlInput by remember { mutableStateOf(false) }
    var manualUrlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var uploadInProgress by remember { mutableStateOf(false) }
    val displayLabel = label.ifBlank { stringResource(R.string.program_image_url_label) }
    val imageSelectedMessage = stringResource(R.string.image_selected_ready_to_upload)
    val imageUploadSuccessMessage = stringResource(R.string.image_upload_success)
    val imageUploadFailedMessage = stringResource(R.string.image_upload_failed)
    val imageUrlSavedMessage = stringResource(R.string.free_image_url_saved)
    val imageUrlHttpsRequiredMessage = stringResource(R.string.image_url_https_required)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedUri = it
            showUrlInput = true
            manualUrlInput = ""
            Toast.makeText(context, imageSelectedMessage, Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, BrandAccent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(20.dp))
                    Text(displayLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = BrandPrimary.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp)) {
                    Text(stringResource(R.string.firebase_storage_badge), color = BrandAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(BrandPanel).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage: Any? = selectedUri ?: uploadedUrl.takeIf { it.isNotBlank() }
                    if (displayImage != null) {
                        AsyncImage(model = displayImage, contentDescription = stringResource(R.string.image_preview), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                            Text(stringResource(R.string.no_image), color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.choose_image_preview), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            uploadInProgress = true
                            val safeFolder = folderPath.trim().trim('/').ifBlank { "admin-media" }
                            val objectRef = FirebaseStorage.getInstance().reference
                                .child("$safeFolder/${UUID.randomUUID()}.jpg")
                            objectRef.putFile(uri)
                                .addOnSuccessListener {
                                    objectRef.downloadUrl
                                        .addOnSuccessListener { downloadUri ->
                                            uploadedUrl = downloadUri.toString()
                                            selectedUri = null
                                            uploadInProgress = false
                                            onUrlGenerated(downloadUri.toString())
                                            Toast.makeText(context, imageUploadSuccessMessage, Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            uploadInProgress = false
                                            Toast.makeText(context, imageUploadFailedMessage, Toast.LENGTH_LONG).show()
                                        }
                                }
                                .addOnFailureListener {
                                    uploadInProgress = false
                                    Toast.makeText(context, imageUploadFailedMessage, Toast.LENGTH_LONG).show()
                                }
                        },
                        enabled = selectedUri != null && !uploadInProgress,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (uploadInProgress) R.string.image_uploading else R.string.upload_to_firebase_storage), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showUrlInput = !showUrlInput }, modifier = Modifier.height(30.dp)) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (showUrlInput) R.string.hide_image_url else R.string.enter_free_image_url), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showUrlInput || uploadedUrl.isBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.image_url_fallback_hint), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                    OutlinedTextField(
                        value = manualUrlInput,
                        onValueChange = { manualUrlInput = it },
                        label = { Text(stringResource(R.string.direct_https_image_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.2f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Button(
                        onClick = {
                            val url = manualUrlInput.trim()
                            if (url.startsWith("https://")) {
                                uploadedUrl = url
                                onUrlGenerated(url)
                                Toast.makeText(context, imageUrlSavedMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, imageUrlHttpsRequiredMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.save_image_url), color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            if (uploadedUrl.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Green.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.firebase_storage_note), color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
