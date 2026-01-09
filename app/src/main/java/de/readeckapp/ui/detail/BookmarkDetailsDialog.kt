package de.readeckapp.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.readeckapp.R
import de.readeckapp.domain.model.Bookmark
import java.time.format.DateTimeFormatter

@Composable
fun BookmarkDetailsDialog(
    bookmark: Bookmark,
    onDismissRequest: () -> Unit,
    onLabelsUpdate: (List<String>) -> Unit = {}
) {
    var labels by remember { mutableStateOf(bookmark.labels.toMutableList()) }
    var newLabelInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.detail_dialog_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_search)
                        )
                    }
                }

                // Metadata Section
                MetadataField(
                    label = stringResource(R.string.detail_type),
                    value = bookmark.type.displayName()
                )

                if (!bookmark.lang.isNullOrBlank() && bookmark.lang != "Unknown") {
                    MetadataField(
                        label = stringResource(R.string.detail_language),
                        value = bookmark.lang
                    )
                }

                if (bookmark.wordCount != null && bookmark.wordCount!! > 0) {
                    MetadataField(
                        label = stringResource(R.string.detail_word_count),
                        value = "${bookmark.wordCount} words"
                    )
                }

                if (bookmark.readingTime != null && bookmark.readingTime!! > 0) {
                    MetadataField(
                        label = stringResource(R.string.detail_reading_time),
                        value = "${bookmark.readingTime} ${stringResource(R.string.detail_minutes_short)}"
                    )
                }

                if (bookmark.authors.isNotEmpty()) {
                    MetadataField(
                        label = "Authors",
                        value = bookmark.authors.joinToString(", ")
                    )
                }

                if (!bookmark.description.isNullOrBlank()) {
                    MetadataField(
                        label = "Description",
                        value = bookmark.description
                    )
                }

                // Labels Section
                LabelsSection(
                    labels = labels,
                    newLabelInput = newLabelInput,
                    onNewLabelChange = { newLabelInput = it },
                    onAddLabel = {
                        if (newLabelInput.isNotBlank() && !labels.contains(newLabelInput)) {
                            labels.add(newLabelInput)
                            newLabelInput = ""
                            keyboardController?.hide()
                        }
                    },
                    onRemoveLabel = { label ->
                        labels.remove(label)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Save Button
                Button(
                    onClick = {
                        onLabelsUpdate(labels)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun MetadataField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelsSection(
    labels: List<String>,
    newLabelInput: String,
    onNewLabelChange: (String) -> Unit,
    onAddLabel: () -> Unit,
    onRemoveLabel: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.detail_labels),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Existing labels
        if (labels.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labels.forEach { label ->
                    LabelChip(
                        label = label,
                        onRemove = {
                            onRemoveLabel(label)
                        }
                    )
                }
            }
        }

        // Input field for new label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newLabelInput,
                onValueChange = onNewLabelChange,
                placeholder = { Text(stringResource(R.string.detail_label_placeholder)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onAddLabel() }
                ),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onAddLabel,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.detail_add_label))
            }
        }
    }
}

@Composable
private fun LabelChip(
    label: String,
    onRemove: () -> Unit = {}
) {
    Card(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove label",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun Bookmark.Type.displayName(): String {
    return when (this) {
        Bookmark.Type.Article -> "Article"
        Bookmark.Type.Picture -> "Picture"
        Bookmark.Type.Video -> "Video"
    }
}
