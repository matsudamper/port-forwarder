package terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Row
import com.jakewharton.mosaic.ui.Text

@Composable
internal fun MosaicRoot(
    uiState: TerminalUiState,
) {
    Column {
        val ktorStatusText = remember(uiState.ktorStatus) {
            if (uiState.ktorStatus) {
                "active"
            } else {
                "down"
            }
        }
        Text("Ktor Status: $ktorStatusText")
        Text("count: ${uiState.count}")

        Text("x: ${uiState.x}")
        Text("y: ${uiState.y}")

        val logScreen = uiState.logScreen
        Text("logScreen: $logScreen")
        if (logScreen != null) {
            logScreen.line.forEach {
                Text(
                    value = it,
                )
            }
            Row {
                Text(
                    value = "Out",
                    background = if (logScreen.type == TerminalUiState.LogScreen.Type.Out) {
                        Color.White
                    } else {
                        null
                    },
                    color = if (logScreen.type == TerminalUiState.LogScreen.Type.Out) {
                        Color.Black
                    } else {
                        null
                    },
                )
                Text("  /  ")
                Text(
                    value = "Error",
                    background = if (logScreen.type == TerminalUiState.LogScreen.Type.Error) {
                        Color.White
                    } else {
                        null
                    },
                    color = if (logScreen.type == TerminalUiState.LogScreen.Type.Error) {
                        Color.Black
                    } else {
                        null
                    },
                )
            }
        } else {
            when (uiState.screen) {
                is TerminalUiState.Screen.Root -> {
                    uiState.screen.items.forEach {
                        Text(
                            value = it.name,
                            color = if (it.selected) {
                                Color.Green
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
