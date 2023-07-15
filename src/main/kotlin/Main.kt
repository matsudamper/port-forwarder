import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.charleskorn.kaml.Yaml
import com.jakewharton.mosaic.runMosaicBlocking
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import terminal.MosaicRoot
import terminal.TerminalViewModel
import java.io.File
import kotlin.system.exitProcess


suspend fun main(args: Array<String>) {
//    System.setProperty("logback.configurationFile", "logback.xml")
    val options = OptionParser(args.toList()).get()
    val port = options["port"]?.toIntOrNull()
    val configFile = options["config"]

    println("start server...")
    println("port=$port")
    println("configFile=$configFile")

    if (configFile == null) {
        System.err.println("configFile is not set")
        exitProcess(1)
    }

    val config = Yaml.default.decodeFromString<Config>(File(configFile).readText())
    runBlocking {
        launch {
            val forward = createForward(config)
            Global.forwards.clear()
            Global.forwards.addAll(forward)

            val forwardJobs = Global.forwards.map {
                launch {
                    runCatching {
                        it.start()
                        it.collectText()
                    }.onFailure {
                        throw RuntimeException("$it", it)
                    }
                }
            }

            Runtime.getRuntime().addShutdownHook(
                    object : Thread() {
                        override fun run() {
                            println("shutdown: size=${Global.forwards.size}")
                            forwardJobs.forEach { it.cancel() }
                            Global.forwards.forEach { it.kill() }
                            println("shutdown: finish")
                        }
                    }
            )
        }
        val terminalViewModel = TerminalViewModel(
                coroutineScope = this,
                forwards = Global.forwards,
                listener = terminalListener
        ).also {
            it.init()
        }
        if (port != null) {
            launch(Job()) {
                embeddedServer(
                        CIO,
                        port = port,
                        module = {
                            myApplicationModule(
                                    onStart = {
                                        terminalViewModel.ktorStatus(true)
                                    },
                                    onStop = {
                                        terminalViewModel.ktorStatus(false)
                                    }
                            )
                        },
                        configure = {

                        },
                ).start(wait = false)
            }
        }

        launch {
            runMosaicBlocking {
                var count by mutableStateOf(0)

                setContent {
                    MosaicRoot(
                            uiState = terminalViewModel.uiStateFlow.collectAsState().value,
                    )
                }
                for (item in 0 until 5) {
                    count++
                }
                awaitCancellation()
            }
        }
        awaitCancellation()
    }
}

private val terminalListener = object : TerminalViewModel.ViewModelEventListener {
    override fun exit() {
        stopAndExit()
    }
}

private fun stopAndExit() {
    Global.forwards.forEach { it.kill() }
    exitProcess(0)
}

private fun createForward(config: Config): List<Forward> {
    val destination = if (config.destination.command != null) {
        ProcessBuilder("bash", "-c", config.destination.command).let {
            val process = it.start()
            process.waitFor()
            process.inputReader().readText().trim()
        }.takeIf { it.isNotBlank() }
    } else {
        config.destination.text
    }

    if (destination == null) {
        throw NullPointerException("destination is null")
    }

    val key = if (config.key.command != null) {
        Runtime.getRuntime().exec(config.key.command).let {
            it.waitFor()
            it.inputReader().readText()
        }.takeIf { it.isNotBlank() }
    } else {
        config.key.text
    }

    if (key == null) {
        throw NullPointerException("key is null")
    }

    return config.forward.map {
        val port = it.key
        val host = it.value

        val serverHost: String
        val serverPort: Int
        host.split(":").also { hostAndPort ->
            serverHost = hostAndPort[0]
            serverPort = hostAndPort[1].toInt()
        }

        Forward(
                localPort = port,
                serverHost = serverHost,
                serverPort = serverPort,
                keyPath = key,
                destination = destination,
        )
    }
}
