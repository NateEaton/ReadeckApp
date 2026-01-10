package de.readeckapp.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import de.readeckapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkDetailsDialog(
    bookmark: BookmarkDetailViewModel.Bookmark,
    onDismissRequest: () -> Unit,
    onLabelsUpdate: (List<String>) -> Unit = {}
) {
    var labels by remember { mutableStateOf(bookmark.labels.toMutableList()) }
    var newLabelInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_dialog_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    if (newLabelInput.isNotBlank()) {
                        // Split on commas and trim each label
                        val newLabels = newLabelInput.split(',')
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !labels.contains(it) }

                        labels.addAll(newLabels)
                        newLabelInput = ""
                        keyboardController?.hide()
                        if (newLabels.isNotEmpty()) {
                            onLabelsUpdate(labels)
                        }
                    }
                },
                onRemoveLabel = { label ->
                    labels.remove(label)
                    onLabelsUpdate(labels)
                }
            )
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
        OutlinedTextField(
            value = newLabelInput,
            onValueChange = onNewLabelChange,
            placeholder = { Text(stringResource(R.string.detail_label_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onAddLabel() }
            ),
            textStyle = MaterialTheme.typography.bodySmall
        )
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

private fun BookmarkDetailViewModel.Bookmark.Type.displayName(): String {
    return when (this) {
        BookmarkDetailViewModel.Bookmark.Type.ARTICLE -> "Article"
        BookmarkDetailViewModel.Bookmark.Type.PHOTO -> "Photo"
        BookmarkDetailViewModel.Bookmark.Type.VIDEO -> "Video"
    }
}
