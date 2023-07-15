package net.matsudamper.portfoward

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.charleskorn.kaml.Yaml
import com.jakewharton.mosaic.runMosaicBlocking
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import net.matsudamper.portfoward.server.myApplicationModule
import net.matsudamper.portfoward.terminal.MosaicRoot
import net.matsudamper.portfoward.terminal.TerminalViewModel
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
            val forwards = createForward(config)
            Global.forwards.clear()
            Global.forwards.addAll(forwards)

            val forwardJobs = Global.forwards.map { forward ->
                launch {
                    try {
                        forward.start()
                        forward.collectText()
                    } catch (_: CancellationException) {

                    } catch (e: Throwable) {
                        e.printStackTrace()
                        throw RuntimeException("$e", e)
                    }
                }
            }

            Runtime.getRuntime().addShutdownHook(
                object : Thread() {
                    override fun run() {
                        println("shutdown: size=${Global.forwards.size}")
                        Global.forwards.forEach { it.kill() }
                        forwardJobs.forEach { it.cancel() }
                        println("shutdown: finish")
                    }
                },
            )
        }
        val terminalViewModel = TerminalViewModel(
            coroutineScope = this,
            forwards = Global.forwards,
            listener = terminalListener,
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
                            },
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
        config.key.text?.takeIf { it.isNotBlank() }
    }

    return config.forward.map {
        val local = it.key
        val server = it.value

        val localHost: String
        val localPort: Int
        local.split(":").also { hostAndPort ->
            localHost = hostAndPort[0]
            localPort = hostAndPort[1].toInt()
        }

        val serverHost: String
        val serverPort: Int
        server.split(":").also { hostAndPort ->
            serverHost = hostAndPort[0]
            serverPort = hostAndPort[1].toInt()
        }

        Forward(
            localHost = localHost,
            localPort = localPort,
            serverHost = serverHost,
            serverPort = serverPort,
            keyPath = key,
            destination = destination,
        )
    }
}
