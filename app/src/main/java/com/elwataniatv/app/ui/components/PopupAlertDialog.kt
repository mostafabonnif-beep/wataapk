package com.elwataniatv.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import com.elwataniatv.app.data.model.PopupAlert

@Composable
fun PopupAlertDialog(
    alert: PopupAlert,
    onDismiss: () -> Unit
) {
    if (!alert.active || alert.title.isBlank()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(alert.title) },
        text = { Text(alert.message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(alert.buttonText.ifBlank { stringResource(R.string.ok) })
            }
        }
    )
}
