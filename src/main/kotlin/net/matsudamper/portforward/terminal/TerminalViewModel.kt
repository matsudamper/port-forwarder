package net.matsudamper.portforward.terminal

import androidx.compose.runtime.MutableState
import java.io.Reader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.matsudamper.portforward.Forward
import org.jline.terminal.TerminalBuilder

class TerminalViewModel(
    private val coroutineScope: CoroutineScope,
    private val listener: ViewModelEventListener,
    forwards: MutableList<Forward>,
) {
    private val viewModelStateFlow = MutableStateFlow(
        ViewModelState(
            forwards = forwards,
            x = 0,
            y = 0,
            ktorStatus = true,
            logScreen = null,
        ),
    )
    private val width = 20
    private val height = 20
    val uiStateFlow: StateFlow<TerminalUiState> = MutableStateFlow(
        TerminalUiState(
            x = 0,
            y = 0,
            width = width,
            height = height,
            count = 0,
            ktorStatus = true,
            screen = TerminalUiState.Screen.Root(
                items = listOf(),
            ),
            logScreen = null,
        ),
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collect { viewModelState ->
                uiStateFlow.update { uiState ->
                    uiState.copy(
                        y = viewModelState.y,
                        screen = TerminalUiState.Screen.Root(
                            viewModelState.forwards.map { item ->
                                "${item.localPort} -> ${item.serverHost}:${item.serverPort}"
                            }
                                .plus("Exit")
                                .mapIndexed { index, text ->
                                    TerminalUiState.Screen.Root.Item(
                                        name = text,
                                        selected = index == viewModelState.y,
                                    )
                                },
                        ),
                        ktorStatus = viewModelState.ktorStatus,
                        logScreen = run logScreen@{
                            val logScreen = viewModelState.logScreen ?: return@logScreen null

                            TerminalUiState.LogScreen(
                                line = logScreen.item.input.value
                                    .drop(logScreen.index)
                                    .take(LogLength),
                                type = logScreen.type,
                            )
                        },
                    )
                }
            }
        }
    }
    private val listMaxSize: Int
        get() {
            return viewModelStateFlow.value.forwards.size.minus(1)
                .plus(1) // item: Exit
        }

    fun init() {
        coroutineScope.launch(Dispatchers.IO) {
            val terminal = TerminalBuilder.terminal()
            terminal.enterRawMode()
            val reader = terminal.reader()

            while (true) {
                val logScreen = viewModelStateFlow.value.logScreen
                if (logScreen != null) {
                    val detect = detectButton(reader)

                    when (detect) {
                        Arrow.Left,
                        Arrow.Right,
                        -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    logScreen = it.logScreen?.copy(
                                        type = when (logScreen.type) {
                                            TerminalUiState.LogScreen.Type.Out -> TerminalUiState.LogScreen.Type.Error
                                            TerminalUiState.LogScreen.Type.Error -> TerminalUiState.LogScreen.Type.Out
                                        },
                                    ),
                                )
                            }
                        }

                        Arrow.Up -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    logScreen = logScreen.copy(
                                        index = logScreen.index.minus(1)
                                            .coerceAtMost(logScreen.item.input.value.size - 1)
                                            .coerceAtLeast(0),
                                    ),
                                )
                            }
                        }

                        Arrow.Down -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    logScreen = logScreen.copy(
                                        index = logScreen.index.plus(1)
                                            .coerceAtMost(logScreen.item.input.value.size - 1)
                                            .coerceAtLeast(0),
                                    ),
                                )
                            }
                        }

                        Arrow.Enter -> Unit
                        Arrow.Backspace -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    logScreen = null,
                                )
                            }
                        }

                        null -> Unit
                    }
                } else {
                    when (detectButton(reader)) {
                        Arrow.Left -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    x = (it.x - 1).coerceAtLeast(0),
                                )
                            }
                        }

                        Arrow.Right -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    x = (it.x + 1).coerceAtMost(width),
                                )
                            }
                        }

                        Arrow.Up -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    y = (it.y - 1)
                                        .coerceAtLeast(0)
                                        .coerceAtMost(listMaxSize),
                                )
                            }
                        }

                        Arrow.Down -> {
                            viewModelStateFlow.update {
                                it.copy(
                                    y = (it.y + 1)
                                        .coerceAtLeast(0)
                                        .coerceAtMost(listMaxSize),
                                )
                            }
                        }

                        Arrow.Enter -> {
                            val index = viewModelStateFlow.value.y
                            val forward = viewModelStateFlow.value.forwards.getOrNull(index)
                            if (forward != null) {
                                viewModelStateFlow.update {
                                    it.copy(
                                        logScreen = ViewModelState.LogScreen(
                                            item = forward,
                                            index = (forward.input.value.size - LogLength)
                                                .coerceAtMost(forward.input.value.size - 1)
                                                .coerceAtLeast(0),
                                            type = TerminalUiState.LogScreen.Type.Out,
                                        ),
                                    )
                                }
                            } else {
                                listener.exit()
                            }
                        }

                        Arrow.Backspace -> Unit
                        null -> Unit
                    }
                }
            }
        }
    }

    fun ktorStatus(value: Boolean) {
        viewModelStateFlow.update {
            it.copy(ktorStatus = value)
        }
    }

    private fun detectButton(reader: Reader): Arrow? {
        return when (reader.read()) {
            // Arrow
            27 -> {
                when (reader.read()) {
                    91 -> {
                        when (reader.read()) {
                            65 -> Arrow.Up
                            66 -> Arrow.Down
                            67 -> Arrow.Right
                            68 -> Arrow.Left
                            else -> null
                        }
                    }

                    else -> null
                }
            }

            13 -> Arrow.Enter
            127 -> Arrow.Backspace
            else -> null
        }
    }

    interface ViewModelEventListener {
        fun exit()
    }

    enum class Arrow {
        Left,
        Right,
        Up,
        Down,
        Enter,
        Backspace,
    }

    private data class ViewModelState(
        val forwards: MutableList<Forward>,
        val x: Int,
        val y: Int,
        val ktorStatus: Boolean,
        val logScreen: LogScreen?,
    ) {
        data class LogScreen(
            val item: Forward,
            val index: Int,
            val type: TerminalUiState.LogScreen.Type,
        )
    }

    companion object {
        private val LogLength: Int = 10
    }
}

private fun <T> MutableState<T>.update(block: (T) -> T) {
    value = block(value)
}
