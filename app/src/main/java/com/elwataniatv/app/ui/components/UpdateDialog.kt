package com.elwataniatv.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R

@Composable
fun UpdateDialog(
    forceUpdate: Boolean,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!forceUpdate) onDismiss() },
        title = { Text(stringResource(if (forceUpdate) R.string.update_required else R.string.update_available)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.update_now)) } },
        dismissButton = if (!forceUpdate) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) } }
        } else null
    )
}
