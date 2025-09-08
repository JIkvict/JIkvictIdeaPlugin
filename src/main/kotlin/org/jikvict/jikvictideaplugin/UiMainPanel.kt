package org.jikvict.jikvictideaplugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import org.jikvict.api.apis.AssignmentControllerApi
import org.jikvict.api.models.AssignmentDto
import org.jikvict.api.models.AssignmentInfo
import org.jikvict.jikvictideaplugin.components.AssignmentDetailsPane
import org.jikvict.jikvictideaplugin.components.AssignmentListPane
import org.jikvict.jikvictideaplugin.services.AssignmentService
import javax.swing.JComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class UiMainPanel {
    fun createPanel(): JComponent = ComposePanel().apply {
        setContent { IdeaMaterialTheme { MainContentRoot() } }
    }
}

private class AssignmentsVm(
    private val api: AssignmentControllerApi = AssignmentControllerApi()
) {
    var isLoading = mutableStateOf(false)
        private set
    var error = mutableStateOf<String?>(null)
        private set
    var assignments = mutableStateOf<List<AssignmentDto>>(emptyList())
        private set
    val infos = mutableStateMapOf<Long, AssignmentInfo>()

    suspend fun loadAll() {
        isLoading.value = true
        error.value = null
        try {
            val list = api.getAll().body()
            assignments.value = list
        } catch (e: Exception) {
            error.value = e.message ?: e.toString()
        } finally {
            isLoading.value = false
        }
    }

    suspend fun ensureInfoLoaded(id: Long) {
        if (infos.containsKey(id)) return
        try {
            val info = api.getAssignmentInfoForUser(id).body()
            infos[id] = info
        } catch (_: Exception) {
        }
    }
}

enum class UiScreen { LIST, DETAILS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContentRoot() {
    val state = remember { AssignmentsVm() }
    val scope = rememberCoroutineScope()
    val service = remember { AssignmentService() }

    val currentScreen = remember { mutableStateOf(UiScreen.LIST) }
    val selected = remember { mutableStateOf<AssignmentDto?>(null) }

    LaunchedEffect(Unit) { state.loadAll() }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen.value) {
            UiScreen.LIST -> Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Assignments") },
                    actions = {
                        IconButton(onClick = { scope.launch { state.loadAll() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.isLoading.value -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        state.error.value != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${'$'}{state.error.value}") }
                        else -> AssignmentListPane(
                            assignments = state.assignments.value,
                            onAssignmentClick = { a ->
                                selected.value = a
                                scope.launch {
                                    state.ensureInfoLoaded(a.id)
                                    currentScreen.value = UiScreen.DETAILS
                                }
                            },
                            assignmentInfos = state.infos.mapKeys { it.key.toInt() }
                        )
                    }
                }
            }
            UiScreen.DETAILS -> Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(selected.value?.title ?: "Details") },
                    navigationIcon = {
                        IconButton(onClick = { currentScreen.value = UiScreen.LIST }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { state.loadAll() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = {
                            val a = selected.value
                            if (a != null) {
                                scope.launch { service.downloadAndOpenProject(a) }
                            }
                        }) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Download and Open Project")
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    val a = selected.value
                    if (a == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No selection") }
                    } else {
                        AssignmentDetailsPane(
                            assignment = a,
                            info = state.infos[a.id],
                            service = service,
                        )
                    }
                }
            }
        }
    }
}
