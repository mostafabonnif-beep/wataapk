package com.elwataniatv.app.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.elwataniatv.app.R
import androidx.compose.ui.unit.dp
import com.elwataniatv.app.ui.theme.BrandPrimary

/**
 * إرسال ملاحظات لإدارة القناة — pure presentation dialog. Submission
 * (validation, network call, toasts, reset) is handled by the caller via
 * [onSubmit], which receives the current draft text.
 */
@Composable
fun FeedbackDialog(
    feedbackText: String,
    onFeedbackTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feedback_dialog_title), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = feedbackText,
                onValueChange = onFeedbackTextChange,
                placeholder = { Text(stringResource(R.string.feedback_dialog_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedbackText.isNotBlank()) {
                        onSubmit(feedbackText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
