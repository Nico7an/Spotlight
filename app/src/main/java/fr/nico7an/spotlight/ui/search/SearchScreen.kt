package fr.nico7an.spotlight.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import fr.nico7an.spotlight.data.AppEntry
import fr.nico7an.spotlight.data.AppRepository
import fr.nico7an.spotlight.data.SearchEngine
import fr.nico7an.spotlight.data.UsageStore
import fr.nico7an.spotlight.ui.theme.LocalSpotlightPalette

private sealed interface ResultItem {
    val key: String

    data class App(val app: AppEntry) : ResultItem {
        override val key: String get() = app.key
    }

    data class StoreSearch(val query: String) : ResultItem {
        override val key: String get() = "store:$query"
    }
}

@Composable
fun SearchScreen(
    repository: AppRepository,
    usage: UsageStore,
    showSuggestions: Boolean,
    session: Int,
    onLaunchApp: (AppEntry) -> Unit,
    onStoreSearch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalSpotlightPalette.current
    val apps by repository.apps.collectAsState()
    var query by remember(session) { mutableStateOf(TextFieldValue("")) }
    var selected by remember(session) { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val items: List<ResultItem> = remember(query.text, apps, showSuggestions) {
        if (query.text.isBlank()) {
            if (showSuggestions) usage.suggestions(apps).map(ResultItem::App) else emptyList()
        } else {
            SearchEngine.search(apps, query.text) { usage.boost(it.key) }
                .map<AppEntry, ResultItem>(ResultItem::App)
                .ifEmpty { listOf(ResultItem.StoreSearch(query.text.trim())) }
        }
    }
    val current = if (items.isEmpty()) -1 else selected.coerceIn(0, items.lastIndex)

    fun activate(index: Int) {
        when (val item = items.getOrNull(index)) {
            is ResultItem.App -> onLaunchApp(item.app)
            is ResultItem.StoreSearch -> onStoreSearch(item.query)
            null -> Unit
        }
    }

    fun move(delta: Int, wrap: Boolean = true) {
        if (items.isEmpty()) return
        val target = current + delta
        selected = if (wrap) target.mod(items.size) else target.coerceIn(0, items.lastIndex)
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return when (event.key) {
            Key.DirectionDown -> { move(1); true }
            Key.DirectionUp -> { move(-1); true }
            Key.Tab -> { move(if (event.isShiftPressed) -1 else 1); true }
            Key.PageDown -> { move(5, wrap = false); true }
            Key.PageUp -> { move(-5, wrap = false); true }
            Key.Enter, Key.NumPadEnter -> { activate(current); true }
            Key.Escape -> {
                if (query.text.isNotEmpty()) {
                    query = TextFieldValue("")
                    selected = 0
                } else {
                    onDismiss()
                }
                true
            }
            else -> false
        }
    }

    LaunchedEffect(session) {
        focusRequester.requestFocus()
        keyboard?.show()
        listState.scrollToItem(0)
    }

    // Garde la sélection visible lors de la navigation au clavier.
    LaunchedEffect(current, items) {
        if (current < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val fullyVisible = visible.filter { it.offset >= 0 && it.offset + it.size <= info.viewportEndOffset }
        val first = fullyVisible.firstOrNull()?.index ?: visible.first().index
        val last = fullyVisible.lastOrNull()?.index ?: visible.first().index
        when {
            current < first -> listState.animateScrollToItem(current)
            current > last -> listState.animateScrollToItem(current - (last - first))
        }
    }

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

    CompositionLocalProvider(LocalContentColor provides palette.primaryText) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
                .imePadding(),
        ) {
            val panelWidth = min(maxWidth * 0.92f, 680.dp)
            val shape = RoundedCornerShape(28.dp)

            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = maxHeight * 0.12f)
                    .width(panelWidth)
                    .graphicsLayer {
                        alpha = appear.value
                        val scale = 0.96f + 0.04f * appear.value
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(shape)
                    .background(palette.panel)
                    .border(1.dp, palette.panelStroke, shape)
                    // Les taps dans le panneau ne doivent pas fermer la fenêtre.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .animateContentSize(tween(180, easing = FastOutSlowInEasing)),
            ) {
                SearchField(
                    query = query,
                    onQueryChange = {
                        query = it
                        selected = 0
                    },
                    focusRequester = focusRequester,
                    onKey = ::onKey,
                    onGo = { activate(current) },
                    onClear = {
                        query = TextFieldValue("")
                        selected = 0
                        focusRequester.requestFocus()
                    },
                )

                if (items.isNotEmpty()) {
                    HorizontalDivider(thickness = 1.dp, color = palette.panelStroke)
                    SectionHeader(
                        when {
                            query.text.isBlank() -> "Suggestions"
                            items.first() is ResultItem.StoreSearch -> "Aucune app trouvée"
                            else -> "Applications"
                        },
                    )
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                            ResultRow(
                                item = item,
                                selected = index == current,
                                query = query.text.trim(),
                                repository = repository,
                                onClick = { activate(index) },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Footer()
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onKey: (KeyEvent) -> Boolean,
    onGo: () -> Unit,
    onClear: () -> Unit,
) {
    val palette = LocalSpotlightPalette.current
    val textStyle = TextStyle(fontSize = 22.sp, color = palette.primaryText, fontWeight = FontWeight.Normal)
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.secondaryText, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.text.isEmpty()) {
                Text("Rechercher une app", style = textStyle.copy(color = palette.placeholder), maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent(onKey),
            )
        }
        AnimatedVisibility(
            visible = query.text.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(palette.panelStroke)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Effacer", tint = palette.secondaryText, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalSpotlightPalette.current.secondaryText,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun ResultRow(
    item: ResultItem,
    selected: Boolean,
    query: String,
    repository: AppRepository,
    onClick: () -> Unit,
) {
    val palette = LocalSpotlightPalette.current
    val background by animateColorAsState(
        targetValue = if (selected) palette.accent else Color.Transparent,
        animationSpec = tween(110),
        label = "rowBackground",
    )
    val textColor = if (selected) palette.onAccent else palette.primaryText

    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is ResultItem.App -> {
                AppIcon(item.app, repository, 36.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = highlight(item.app.label, query),
                    color = textColor,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            is ResultItem.StoreSearch -> {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.2f) else palette.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Search, null, tint = if (selected) palette.onAccent else palette.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Rechercher « ${item.query} » sur le Play Store",
                    color = textColor,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            KeyCap("↵", palette.onAccent.copy(alpha = 0.85f), palette.onAccent.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun AppIcon(app: AppEntry, repository: AppRepository, size: Dp) {
    val bitmap by produceState(repository.cachedIcon(app), app.key) {
        if (value == null) value = repository.icon(app)
    }
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription = null, modifier = Modifier.size(size), filterQuality = FilterQuality.High)
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
                .background(LocalSpotlightPalette.current.panelStroke),
        )
    }
}

@Composable
private fun Footer() {
    val palette = LocalSpotlightPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(palette.footer)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hint("↑ ↓", "naviguer")
        Hint("↵", "ouvrir")
        Hint("esc", "fermer")
        Spacer(Modifier.weight(1f))
        Text("Spotlight", fontSize = 12.sp, color = palette.placeholder, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Hint(key: String, label: String) {
    val palette = LocalSpotlightPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        KeyCap(key, palette.secondaryText, palette.panelStroke)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = palette.secondaryText)
    }
}

@Composable
fun KeyCap(key: String, color: Color, border: Color) {
    Text(
        text = key,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

private fun highlight(label: String, query: String): AnnotatedString = buildAnnotatedString {
    append(label)
    if (query.isNotEmpty()) {
        val start = label.indexOf(query, ignoreCase = true)
        if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, start + query.length)
    }
}
