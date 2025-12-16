package org.jikvict.jikvictideaplugin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jikvict.api.models.AssignmentDto
import org.jikvict.api.models.AssignmentInfo


@Composable
fun AssignmentListPane(
    assignments: List<AssignmentDto>,
    onAssignmentClick: (AssignmentDto) -> Unit,
    assignmentInfos: Map<Int, AssignmentInfo> = emptyMap(),
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(assignments) { assignment ->
                AssignmentListItem(
                    assignment = assignment,
                    onClick = { onAssignmentClick(assignment) },
                    assignmentInfo = assignmentInfos[assignment.id.toInt()],
                )
            }
        }
    }
}


@Composable
fun AssignmentListItem(
    assignment: AssignmentDto = AssignmentDto(
        id = 1,
        title = "Some title",
        taskId = 1,
        maxPoints = 20,
        startDate = "2023-01-01T00:00:00Z",
        endDate = "2023-01-01T00:00:00Z",
        timeOutSeconds = 500,
        memoryLimit = 500,
        cpuLimit = 500,
        pidsLimit = 500,
        isClosed = false,
        maximumAttempts = 3,
        assignmentGroupsIds = listOf()
    ),
    onClick: () -> Unit = {},
    assignmentInfo: AssignmentInfo? = null,
) {
    
    
    val shape = RoundedCornerShape(12.dp)

    
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )

    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = colors,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = assignment.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Task #${assignment.taskId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}
