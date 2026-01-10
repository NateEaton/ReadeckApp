package de.readeckapp.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import de.readeckapp.R
import de.readeckapp.domain.model.Bookmark
import de.readeckapp.domain.model.BookmarkListItem
import de.readeckapp.ui.components.ShareBookmarkChooser
import de.readeckapp.ui.navigation.BookmarkDetailRoute
import de.readeckapp.ui.navigation.SettingsRoute
import de.readeckapp.util.openUrlInCustomTab
import kotlinx.coroutines.launch
import androidx.compose.material3.Badge
import de.readeckapp.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkListScreen(navHostController: NavHostController) {
    val viewModel: BookmarkListViewModel = hiltViewModel()
    val navigationEvent = viewModel.navigationEvent.collectAsState()
    val openUrlEvent = viewModel.openUrlEvent.collectAsState()
    val uiState = viewModel.uiState.collectAsState().value
    val createBookmarkUiState = viewModel.createBookmarkUiState.collectAsState().value
    val bookmarkCounts = viewModel.bookmarkCounts.collectAsState()

    // Collect filter states
    val filterState = viewModel.filterState.collectAsState()

    // Collect search states
    val searchQuery = viewModel.searchQuery.collectAsState()
    val isSearchActive = viewModel.isSearchActive.collectAsState()

    // Collect labels
    val labelsWithCounts = viewModel.labelsWithCounts.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditingLabel by remember { mutableStateOf(false) }
    var editedLabelName by remember { mutableStateOf("") }
    var pendingDeleteLabel by remember { mutableStateOf<String?>(null) }
    var deleteLabelJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    val isLoading by viewModel.loadBookmarksIsRunning.collectAsState()

    // UI event handlers (pass filter update functions)
    val onClickAll = { viewModel.onClickAll() }
    val onClickFilterUnread: () -> Unit = { viewModel.onClickUnread() }
    val onClickFilterArchive: () -> Unit = { viewModel.onClickArchive() }
    val onClickFilterFavorite: () -> Unit = { viewModel.onClickFavorite() }
    val onClickFilterArticles: () -> Unit = { viewModel.onClickArticles() }
    val onClickFilterPictures: () -> Unit = { viewModel.onClickPictures() }
    val onClickFilterVideos: () -> Unit = { viewModel.onClickVideos() }
    val onClickLabel: (String) -> Unit = { label ->
        viewModel.onClickLabel(label)
    }
    val onClickSettings: () -> Unit = { viewModel.onClickSettings() }
    val onClickBookmark: (String) -> Unit = { bookmarkId -> viewModel.onClickBookmark(bookmarkId) }
    val onClickDelete: (String) -> Unit = { bookmarkId ->
        viewModel.onDeleteBookmark(bookmarkId)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Bookmark deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long // 10 seconds
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.onCancelDeleteBookmark()
            }
        }
    }
    val onClickMarkRead: (String, Boolean) -> Unit = { bookmarkId, isRead -> viewModel.onToggleMarkReadBookmark(bookmarkId, isRead) }
    val onClickFavorite: (String, Boolean) -> Unit = { bookmarkId, isFavorite -> viewModel.onToggleFavoriteBookmark(bookmarkId, isFavorite) }
    val onClickArchive: (String, Boolean) -> Unit = { bookmarkId, isArchived -> viewModel.onToggleArchiveBookmark(bookmarkId, isArchived) }
    val onClickOpenInBrowser: (String) -> Unit = { url -> viewModel.onClickOpenInBrowser(url) }
    val onClickShareBookmark: (String) -> Unit = { url -> viewModel.onClickShareBookmark(url) }

    LaunchedEffect(key1 = navigationEvent.value) {
        navigationEvent.value?.let { event ->
            when (event) {
                is BookmarkListViewModel.NavigationEvent.NavigateToSettings -> {
                    navHostController.navigate(SettingsRoute)
                    scope.launch { drawerState.close() }
                }

                is BookmarkListViewModel.NavigationEvent.NavigateToBookmarkDetail -> {
                    navHostController.navigate(BookmarkDetailRoute(event.bookmarkId))
                }
            }
            viewModel.onNavigationEventConsumed() // Consume the event
        }
    }

    val context = LocalContext.current
    LaunchedEffect(key1 = openUrlEvent.value) {
        openUrlInCustomTab(context, openUrlEvent.value)
        viewModel.onOpenUrlEventConsumed()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.all)
                        ) },
                        icon = { Icon(Icons.Outlined.Bookmarks, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.total.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value == BookmarkListViewModel.FilterState(),
                        onClick = {
                            onClickAll()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.unread)
                        ) },
                        icon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null)},
                        badge = {
                            bookmarkCounts.value.unread.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.unread == true,
                        onClick = {
                            onClickFilterUnread()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.archive)
                        ) },
                        icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.archived.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.archived == true,
                        onClick = {
                            onClickFilterArchive()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.favorites)
                        ) },
                        icon = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.favorite.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.favorite == true,
                        onClick = {
                            onClickFilterFavorite()
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.articles)
                        ) },
                        icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.article.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.type == Bookmark.Type.Article,
                        onClick = {
                            onClickFilterArticles()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.videos)
                        ) },
                        icon = { Icon(Icons.Outlined.Movie, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.video.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.type == Bookmark.Type.Video,
                        onClick = {
                            onClickFilterVideos()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.pictures)
                        ) },
                        icon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                        badge = {
                            bookmarkCounts.value.picture.let { count ->
                                if (count > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = count.toString()
                                        )
                                    }
                                }
                            }
                        },
                        selected = filterState.value.type == Bookmark.Type.Picture,
                        onClick = {
                            onClickFilterPictures()
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.labels)
                        ) },
                        icon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                        badge = {
                            if (labelsWithCounts.value.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(
                                        text = labelsWithCounts.value.size.toString()
                                    )
                                }
                            }
                        },
                        selected = filterState.value.viewingLabelsList || filterState.value.label != null,
                        onClick = {
                            viewModel.onClickLabelsView()
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(
                            style = Typography.labelLarge,
                            text = stringResource(id = R.string.settings)
                        ) },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        selected = false,
                        onClick = {
                            onClickSettings()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearchActive.value) {
                            TextField(
                                value = searchQuery.value,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = { Text(stringResource(id = R.string.search_bookmarks)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            if (filterState.value.viewingLabelsList) {
                                Text(stringResource(id = R.string.bookmark_labels))
                            } else if (filterState.value.label != null) {
                                if (isEditingLabel) {
                                    TextField(
                                        value = editedLabelName,
                                        onValueChange = { editedLabelName = it },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${stringResource(id = R.string.labels)} / ${filterState.value.label}")
                                    }
                                }
                            } else {
                                val titleRes = when {
                                    filterState.value.unread == true -> R.string.header_unread
                                    filterState.value.archived == true -> R.string.header_archived
                                    filterState.value.favorite == true -> R.string.header_favorites
                                    filterState.value.type == Bookmark.Type.Article -> R.string.articles
                                    filterState.value.type == Bookmark.Type.Video -> R.string.videos
                                    filterState.value.type == Bookmark.Type.Picture -> R.string.pictures
                                    else -> R.string.header_all
                                }
                                Text(stringResource(id = titleRes))
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive.value) {
                            IconButton(
                                onClick = { viewModel.onSearchActiveChange(false) }
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(id = R.string.close_search)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = stringResource(id = R.string.menu)
                                )
                            }
                        }
                    },
                    actions = {
                        if (filterState.value.label != null && !isSearchActive.value) {
                            // Show edit/check icon when a label is selected
                            if (isEditingLabel) {
                                IconButton(
                                    onClick = {
                                        // Save the edited label
                                        if (editedLabelName.isNotBlank() && editedLabelName != filterState.value.label) {
                                            viewModel.onRenameLabel(filterState.value.label!!, editedLabelName)
                                        }
                                        isEditingLabel = false
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Save"
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        editedLabelName = filterState.value.label ?: ""
                                        isEditingLabel = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(id = R.string.edit_label)
                                    )
                                }
                            }
                        } else if (!isSearchActive.value && !filterState.value.viewingLabelsList) {
                            IconButton(
                                onClick = { viewModel.onSearchActiveChange(true) }
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(id = R.string.search)
                                )
                            }
                        } else if (searchQuery.value.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onClearSearch() }
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = stringResource(id = R.string.clear_search)
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.openCreateBookmarkDialog() }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add_bookmark)
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Show Delete button when a label is selected
                if (filterState.value.label != null && !isEditingLabel) {
                    val labelDeletedMessageFormat = stringResource(R.string.label_deleted)
                    val currentLabel = filterState.value.label!!

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Cancel any existing delete operation
                                deleteLabelJob?.cancel()

                                // Set pending delete
                                pendingDeleteLabel = currentLabel

                                // Show snackbar with undo option
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = labelDeletedMessageFormat.format(currentLabel),
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Long
                                    )

                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        // User clicked undo, cancel the deletion
                                        deleteLabelJob?.cancel()
                                        pendingDeleteLabel = null
                                    }
                                }

                                // Schedule the actual deletion after 5 seconds
                                deleteLabelJob = scope.launch {
                                    kotlinx.coroutines.delay(5000)
                                    if (pendingDeleteLabel == currentLabel) {
                                        viewModel.onDeleteLabel(currentLabel)
                                        pendingDeleteLabel = null
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RectangleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(id = R.string.delete_label))
                        }
                    }
                }

                // Show labels list if viewing labels, otherwise show bookmarks list
                if (filterState.value.viewingLabelsList) {
                    LabelsListView(
                        labels = labelsWithCounts.value,
                        onLabelSelected = { label ->
                            onClickLabel(label)
                        }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { viewModel.onPullToRefresh() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (uiState) {
                        is BookmarkListViewModel.UiState.Empty -> {
                            EmptyScreen(messageResource = uiState.messageResource)
                        }
                        is BookmarkListViewModel.UiState.Success -> {
                        LaunchedEffect(key1 = uiState.updateBookmarkState) {
                            uiState.updateBookmarkState?.let { result ->
                                val message = when (result) {
                                    is BookmarkListViewModel.UpdateBookmarkState.Success -> {
                                        "success"
                                    }

                                    is BookmarkListViewModel.UpdateBookmarkState.Error -> {
                                        result.message
                                    }
                                }
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                        BookmarkListView(
                            bookmarks = uiState.bookmarks,
                            onClickBookmark = onClickBookmark,
                            onClickDelete = onClickDelete,
                            onClickArchive = onClickArchive,
                            onClickFavorite = onClickFavorite,
                            onClickMarkRead = onClickMarkRead,
                            onClickOpenInBrowser = onClickOpenInBrowser,
                            onClickShareBookmark = onClickShareBookmark
                        )
                        // Consumes a shareIntent and creates the corresponding share dialog
                        ShareBookmarkChooser(
                            context = LocalContext.current,
                            intent = viewModel.shareIntent.collectAsState().value,
                            onShareIntentConsumed = { viewModel.onShareIntentConsumed() }
                        )
                    }
                }
                }
                }
            }

            // Show the CreateBookmarkDialog based on the state
            when (createBookmarkUiState) {
                is BookmarkListViewModel.CreateBookmarkUiState.Open -> {
                    CreateBookmarkDialog(
                        onDismiss = { viewModel.closeCreateBookmarkDialog() },
                        title = createBookmarkUiState.title,
                        url = createBookmarkUiState.url,
                        urlError = createBookmarkUiState.urlError,
                        isCreateEnabled = createBookmarkUiState.isCreateEnabled,
                        onTitleChange = { viewModel.updateCreateBookmarkTitle(it) },
                        onUrlChange = { viewModel.updateCreateBookmarkUrl(it) },
                        onCreateBookmark = { viewModel.createBookmark() }
                    )
                }

                is BookmarkListViewModel.CreateBookmarkUiState.Loading -> {
                    // Show a loading indicator
                    Dialog(onDismissRequest = { viewModel.closeCreateBookmarkDialog() }) {
                        CircularProgressIndicator()
                    }
                }

                is BookmarkListViewModel.CreateBookmarkUiState.Success -> {
                    // Optionally show a success message
                    LaunchedEffect(key1 = createBookmarkUiState) {
                        // Dismiss the dialog after a short delay
                        scope.launch {
                            kotlinx.coroutines.delay(1000)
                            viewModel.closeCreateBookmarkDialog()
                        }
                    }
                }

                is BookmarkListViewModel.CreateBookmarkUiState.Error -> {
                    // Show an error message
                    AlertDialog(
                        onDismissRequest = { viewModel.closeCreateBookmarkDialog() },
                        title = { Text(stringResource(id = R.string.error)) },
                        text = { Text(createBookmarkUiState.message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.closeCreateBookmarkDialog() }) {
                                Text(stringResource(id = R.string.ok))
                            }
                        }
                    )
                }

                is BookmarkListViewModel.CreateBookmarkUiState.Closed -> {
                    // Do nothing when the dialog is closed
                }
            }
        }
    }
}

@Composable
fun CreateBookmarkDialog(
    onDismiss: () -> Unit,
    title: String,
    url: String,
    urlError: Int?,
    isCreateEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCreateBookmark: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_new_bookmark)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { onUrlChange(it) },
                    isError = urlError != null,
                    label = { Text(stringResource(id = R.string.url)) },
                    supportingText = {
                        urlError?.let {
                            Text(text = stringResource(it))
                        }
                    }
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { onTitleChange(it) },
                    label = { Text(stringResource(id = R.string.title)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateBookmark()
                },
                enabled = isCreateEnabled
            ) {
                Text(stringResource(id = R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun EmptyScreen(
    modifier: Modifier = Modifier,
    messageResource: Int
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(stringResource(id = messageResource))
        }
    }
}

@Composable
fun BookmarkListView(
    modifier: Modifier = Modifier,
    bookmarks: List<BookmarkListItem>,
    onClickBookmark: (String) -> Unit,
    onClickDelete: (String) -> Unit,
    onClickMarkRead: (String, Boolean) -> Unit,
    onClickFavorite: (String, Boolean) -> Unit,
    onClickArchive: (String, Boolean) -> Unit,
    onClickOpenInBrowser: (String) -> Unit,
    onClickShareBookmark: (String) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(bookmarks) { bookmark ->
            BookmarkCard(
                bookmark = bookmark,
                onClickCard = onClickBookmark,
                onClickDelete = onClickDelete,
                onClickArchive = onClickArchive,
                onClickFavorite = onClickFavorite,
                onClickMarkRead = onClickMarkRead,
                onClickOpenUrl = onClickOpenInBrowser,
                onClickShareBookmark = onClickShareBookmark
            )
        }
    }
}

@Composable
fun LabelsListView(
    modifier: Modifier = Modifier,
    labels: Map<String, Int>,
    onLabelSelected: (String) -> Unit
) {
    if (labels.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.list_view_empty_nothing_to_see),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth()
        ) {
            items(
                items = labels.entries.sortedBy { it.key }.toList(),
                key = { it.key }
            ) { (label, count) ->
                NavigationDrawerItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium
                        ),
                    label = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label)
                            Badge(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(count.toString())
                            }
                        }
                    },
                    selected = false,
                    onClick = {
                        onLabelSelected(label)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun EmptyScreenPreview() {
    EmptyScreen(messageResource = R.string.list_view_empty_nothing_to_see)
}

@Preview(showBackground = true)
@Composable
fun BookmarkListViewPreview() {
    val sampleBookmark = BookmarkListItem(
        id = "1",
        url = "https://example.com",
        title = "Sample Bookmark",
        siteName = "Example",
        type = Bookmark.Type.Article,
        isMarked = false,
        isArchived = false,
        labels = listOf(
            "one",
            "two",
            "three",
            "fourhundretandtwentyone",
            "threethousendtwohundretandfive"
        ),
        isRead = true,
        iconSrc = "https://picsum.photos/seed/picsum/640/480",
        imageSrc = "https://picsum.photos/seed/picsum/640/480",
        thumbnailSrc = "https://picsum.photos/seed/picsum/640/480",
    )
    val bookmarks = listOf(sampleBookmark)

    // Provide a dummy NavHostController for the preview
    val navController = rememberNavController()
    BookmarkListView(
        modifier = Modifier,
        bookmarks = bookmarks,
        onClickBookmark = {},
        onClickDelete = {},
        onClickArchive = { _, _ -> },
        onClickFavorite = { _, _ -> },
        onClickMarkRead = { _, _ -> },
        onClickOpenInBrowser = {},
        onClickShareBookmark = {_ -> }
    )
}
