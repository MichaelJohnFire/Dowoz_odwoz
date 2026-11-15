package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun OnboardingGuardianDialog(
    onConfirm: (fullName: String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            // Un-dismissable: user must enter guardian name on first launch
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        icon = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Autobus szkolny",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        },
        title = {
            Text(
                text = "Pierwsze uruchomienie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Witamy w aplikacji Dowóz-odwóz! Aby rozpocząć rejestrację obecności na trasach szkolnych, podaj Imię i Nazwisko opiekuna / nauczyciela prowadzącego.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Imię i Nazwisko opiekuna") },
                    placeholder = { Text("np. Anna Nowak") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null
                        )
                    },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Wprowadź imię i nazwisko opiekuna")
                        } else {
                            Text("Nazwisko będzie widoczne na listach obecności i raportach.")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_guardian_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.trim().length >= 3) {
                        onConfirm(fullName.trim())
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_submit_button")
            ) {
                Text("Zatwierdź i rozpocznij")
            }
        }
    )
}
