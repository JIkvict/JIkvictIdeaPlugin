package org.jikvict.jikvictideaplugin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jikvict.jikvictideaplugin.services.SettingsState

@Composable
fun SettingsPane(onTokenSaved: () -> Unit = {}) {
    val settings = remember { SettingsState.getInstance() }
    var token by remember { mutableStateOf(settings.jwtToken) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "JWT Token",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Enter your JWT token to authenticate with the JIkvict API. This token will be used for all API requests.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("JWT Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 3,
            maxLines = 10
        )

        Button(
            onClick = {
                settings.jwtToken = token
                saveMessage = "Token saved successfully!"
                onTokenSaved()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Token")
        }

        saveMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "API Configuration",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Base URL: https://jikvict.fiiture.sk",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
