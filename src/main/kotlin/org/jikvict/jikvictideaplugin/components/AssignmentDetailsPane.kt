package org.jikvict.jikvictideaplugin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch
import org.jikvict.api.models.AssignmentDto
import org.jikvict.api.models.AssignmentInfo
import org.jikvict.jikvictideaplugin.services.AssignmentService

@Composable
fun AssignmentDetailsPane(
    assignment: AssignmentDto,
    info: AssignmentInfo?,
    service: AssignmentService,
) {
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    androidx.compose.foundation.text.selection.SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(assignment.title, style = MaterialTheme.typography.titleLarge)
            if (!assignment.description.isNullOrBlank()) {
                Markdown(content = assignment.description)
            }
            HorizontalDivider()
            Text("Task #${assignment.taskId}")
            Text("Max points: ${assignment.maxPoints}")
            Text("Attempts: ${info?.attemptsUsed ?: 0}/${info?.maxAttempts ?: assignment.maximumAttempts}")
            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                if (!isSubmitting) {
                    isSubmitting = true
                    submitError = null
                    scope.launch {
                        try {
                            service.submitAssignment(assignment)
                        } catch (e: Exception) {
                            submitError = e.message ?: e.toString()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            }) {
                Text(if (isSubmitting) "Submitting..." else "Submit")
            }
            submitError?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
}
