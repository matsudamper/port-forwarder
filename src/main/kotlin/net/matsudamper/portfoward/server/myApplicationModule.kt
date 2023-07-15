package net.matsudamper.portfoward.server

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.matsudamper.portfoward.Global

internal fun Application.myApplicationModule(
        onStart: () -> Unit,
        onStop: () -> Unit,
) {
    environment.monitor.subscribe(ApplicationStopping) {
        onStart()
    }
    environment.monitor.subscribe(ApplicationStopping) {
        onStop()
    }
    routing {
        get("/") {
            val response = buildString {
                Global.forwards.onEach {
                    appendLine("Active: localhost:${it.localPort} -> ${it.serverHost}:${it.serverPort}")
                }
            }
            call.respondText(response)
        }
        get("/healthz") {
            call.respondText("ok")
        }
    }
}
