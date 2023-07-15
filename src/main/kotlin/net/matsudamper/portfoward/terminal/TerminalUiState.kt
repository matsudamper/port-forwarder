package net.matsudamper.portfoward.terminal

import androidx.compose.runtime.Immutable

data class TerminalUiState(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val count: Int,
    val ktorStatus: Boolean,
    val screen: Screen,
    val logScreen: LogScreen?,
) {
    @Immutable
    sealed interface Screen {
        data class Root(
            val items: List<Item>,
        ) : Screen {
            data class Item(
                val name: String,
                val selected: Boolean,
            )
        }
    }

    data class LogScreen(
        val line: List<String>,
        val type: Type,
    ) {
        enum class Type {
            Out,
            Error,
        }
    }
}