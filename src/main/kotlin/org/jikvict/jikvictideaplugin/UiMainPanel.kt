package org.jikvict.jikvictideaplugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jikvict.api.apis.AssignmentControllerApi
import org.jikvict.api.models.AssignmentDto
import org.jikvict.api.models.AssignmentInfo
import org.jikvict.jikvictideaplugin.components.AssignmentDetailsPane
import org.jikvict.jikvictideaplugin.components.AssignmentListPane
import org.jikvict.jikvictideaplugin.components.SettingsPane
import org.jikvict.jikvictideaplugin.services.AssignmentService
import org.jikvict.jikvictideaplugin.services.ProjectMetaService
import org.jikvict.jikvictideaplugin.services.SettingsState
import javax.swing.JComponent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class UiMainPanel(private val project: Project) {
    fun createPanel(): JComponent = ComposePanel().apply {
        setContent { IdeaMaterialTheme { MainContentRoot(project) } }
    }
}

private class AssignmentsVm {
    var isLoading = mutableStateOf(false)
        private set
    var error = mutableStateOf<String?>(null)
        private set
    var assignments = mutableStateOf<List<AssignmentDto>>(emptyList())
        private set
    val infos = mutableStateMapOf<Long, AssignmentInfo>()

    private fun getApi(): AssignmentControllerApi {
        val token = SettingsState.getInstance().jwtToken
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(DefaultRequest) {
                headers.append("Authorization", "Bearer $token")
            }
        }
        return AssignmentControllerApi(
            baseUrl = "https://jikvict.fiiture.sk",
            httpClient = httpClient
        )
    }

    suspend fun loadAll() {
        isLoading.value = true
        error.value = null
        try {
            val list = getApi().getAll().body()
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
            val info = getApi().getAssignmentInfoForUser(id).body()
            infos[id] = info
        } catch (_: Exception) {
        }
    }
}

enum class UiScreen { LIST, DETAILS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContentRoot(project: Project) {
    val state = remember { AssignmentsVm() }
    val scope = rememberCoroutineScope()
    val service = remember { AssignmentService() }
    val settings = remember { SettingsState.getInstance() }
    val metaService = remember { ProjectMetaService.getInstance(project) }

    val currentScreen = remember { mutableStateOf(UiScreen.LIST) }
    val selected = remember { mutableStateOf<AssignmentDto?>(null) }
    val hasMetaFile = remember { mutableStateOf(metaService.hasMeta()) }


    LaunchedEffect(Unit) {
        if (settings.jwtToken.isBlank()) {
            currentScreen.value = UiScreen.SETTINGS
        } else {
            state.loadAll()

            val meta = metaService.getMeta()
            if (meta != null) {
                state.ensureInfoLoaded(meta.assignmentId)
                val assignment = state.assignments.value.find { it.id == meta.assignmentId }
                if (assignment != null) {
                    selected.value = assignment
                    currentScreen.value = UiScreen.DETAILS
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen.value) {
            UiScreen.LIST -> Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Assignments") },
                    actions = {
                        IconButton(onClick = { scope.launch { state.loadAll() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(onClick = { currentScreen.value = UiScreen.SETTINGS }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.isLoading.value -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        state.error.value != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${state.error.value}") }
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
                        if (!hasMetaFile.value) {
                            IconButton(onClick = { currentScreen.value = UiScreen.LIST }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
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
                            project = project,
                        )
                    }
                }
            }
            UiScreen.SETTINGS -> Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        if (settings.jwtToken.isNotBlank()) {
                            IconButton(onClick = { currentScreen.value = UiScreen.LIST }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    SettingsPane(
                        onTokenSaved = {
                            if (settings.jwtToken.isNotBlank()) {
                                currentScreen.value = UiScreen.LIST
                                scope.launch { state.loadAll() }
                            }
                        }
                    )
                }
            }
        }
    }
}
