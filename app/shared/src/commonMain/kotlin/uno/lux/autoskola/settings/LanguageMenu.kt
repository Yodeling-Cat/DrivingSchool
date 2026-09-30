package uno.lux.autoskola.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uno.lux.autoskola.i18n.AppLanguage
import uno.lux.autoskola.i18n.strings
import uno.lux.autoskola.ui.WhitePill
import uno.lux.autoskola.ui.theme.Naj

/** Language switcher for the headers: the white pill reading e.g. "SK \u25be". */
@Composable
fun LanguageMenu(current: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.clickable { expanded = true }) {
        WhitePill(height = 38.dp) {
            Text(current.code.uppercase(), style = Naj.type.button, color = Naj.colors.ink)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = strings.settingsLanguage,
                tint = Naj.colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Naj.colors.card,
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            language.nativeName,
                            style = Naj.type.cardTitle,
                            fontWeight = if (language == current) FontWeight.Bold else FontWeight.Normal,
                            color = Naj.colors.ink,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(language)
                    },
                )
            }
        }
    }
}
