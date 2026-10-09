package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/** The app's dropdown: a rounded white card that opens under the button it belongs to. */
@Composable
fun NajDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(18.dp),
        containerColor = Naj.colors.card,
        content = content,
    )
}

/** One choice in a [NajDropdownMenu]. [fontWeight] marks the one that is current, where a menu has one. */
@Composable
fun NajMenuItem(text: String, onClick: () -> Unit, fontWeight: FontWeight? = null) {
    DropdownMenuItem(
        text = { Text(text, style = Naj.type.cardTitle, fontWeight = fontWeight, color = Naj.colors.ink) },
        onClick = onClick,
    )
}

// ── Previews ──

@Preview(widthDp = 300, heightDp = 200)
@Composable
private fun NajMenuItemPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("A CHOICE, AND THE CURRENT ONE")
            NajMenuItem(strings.finishTest, onClick = {})
            NajMenuItem("Slovenčina", onClick = {}, fontWeight = FontWeight.Bold)
        }
    }
}
