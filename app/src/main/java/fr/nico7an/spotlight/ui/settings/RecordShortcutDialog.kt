package fr.nico7an.spotlight.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.nico7an.spotlight.core.Shortcut
import fr.nico7an.spotlight.core.ShortcutRecorder
import fr.nico7an.spotlight.ui.theme.LocalSpotlightPalette
import kotlinx.coroutines.delay

@Composable
fun RecordShortcutDialog(
    serviceEnabled: Boolean,
    onRecorded: (Shortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalSpotlightPalette.current
    val recorder = remember { ShortcutRecorder() }
    val focusRequester = remember { FocusRequester() }

    // Le service d'accessibilité redirige les touches vers l'enregistreur actif.
    DisposableEffect(recorder) {
        ShortcutRecorder.active = recorder
        onDispose { if (ShortcutRecorder.active === recorder) ShortcutRecorder.active = null }
    }
    LaunchedEffect(recorder.result, recorder.cancelled) {
        recorder.result?.let {
            delay(500)
            onRecorded(it)
        }
        if (recorder.cancelled) onDismiss()
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau raccourci") },
        text = {
            Column {
                Text("Appuyez sur la touche ou la combinaison qui ouvrira Spotlight. Échap pour annuler.")
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.field)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { recorder.onKey(it.nativeKeyEvent) }
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = recorder.preview ?: "En attente…",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (recorder.result != null) palette.accent else palette.primaryText,
                    )
                }
                recorder.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                if (!serviceEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Activez d'abord le service : sans lui, le système peut intercepter la touche Meta.",
                        color = palette.secondaryText,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
