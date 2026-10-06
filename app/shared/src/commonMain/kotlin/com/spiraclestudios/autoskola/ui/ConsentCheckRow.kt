package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.spiraclestudios.autoskola.i18n.strings
import com.spiraclestudios.autoskola.ui.theme.Naj

/**
 * A checkbox with words beside it: the documents, and the agreement a parent or guardian gives on
 * behalf of a user under 16.
 *
 * The last one is the reason this is shared rather than written twice. Both places that ask for it —
 * the consent screen and Settings — have to mark it, and the mark is a colour, which reaches nobody
 * who cannot see it; the box therefore also carries [errorMessage] as semantics and announces it,
 * because the press that set the mark leaves the focus down on the button that was pressed.
 *
 * The whole row is pressable, not just the box: the label is what the user reads, and a target the
 * size of a fingerprint is not one a phone should demand.
 */
@Composable
fun ConsentCheckRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    error: Boolean = false,
    errorMessage: String = "",
) {
    val colors = Naj.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            // The row owns the press, so the box does not take a second one.
            onCheckedChange = null,
            modifier = Modifier.semantics {
                if (error) {
                    // `this.` on purpose: `error` alone would be ambiguous with kotlin.error, which
                    // throws instead.
                    this.error(errorMessage)
                    liveRegion = LiveRegionMode.Polite
                }
            },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.primaryDeep,
                uncheckedColor = if (error) colors.wrongOn else colors.muted,
                checkmarkColor = colors.mintGlow,
            ),
        )
        Column(Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                title,
                style = Naj.type.body,
                // Red as well as the box, for anyone the red border alone does not reach.
                color = if (error) colors.wrongOn else colors.ink,
            )
            if (description != null) {
                Text(description, style = Naj.type.rowSubtitle, color = colors.muted)
            }
        }
    }
}

// ── Previews ──

@Preview(widthDp = 400)
@Composable
private fun ConsentCheckRowPreview() {
    AppPreview {
        PreviewBoard {
            PreviewLabel("Unticked")
            ConsentCheckRow(
                title = strings.consentParentalConsent,
                description = strings.consentParentalConsentDescription,
                checked = false,
                onCheckedChange = {},
            )
            PreviewLabel("Ticked")
            ConsentCheckRow(
                title = strings.consentParentalConsent,
                description = strings.consentParentalConsentDescription,
                checked = true,
                onCheckedChange = {},
            )
            PreviewLabel("Marked, because a button was pressed without it")
            ConsentCheckRow(
                title = strings.consentParentalConsent,
                description = strings.consentParentalConsentDescription,
                checked = false,
                error = true,
                errorMessage = strings.consentAgreeRequired,
                onCheckedChange = {},
            )
            PreviewLabel("No description")
            ConsentCheckRow(title = strings.consentAgree, checked = false, onCheckedChange = {})
        }
    }
}
