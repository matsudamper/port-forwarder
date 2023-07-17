import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import net.matsudamper.portforward.main
import net.matsudamper.portforward.server.myApplicationModule
import org.junit.jupiter.api.Test

class GenerateReflectionConfig {
    @Test
    fun main() {
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            main(
                arrayOf(
                    "--port=8080",
                    "--config=config.yml",
                ),
            )
        }

        runCatching {
            runBlocking {
                withTimeout(5000) {
                    awaitCancellation()
                }
            }
        }

        job.cancel()
    }

    @Test
    fun startAppServer(): Unit = runBlocking {
        runCatching {
            withTimeout(5000) {
                val scope = this
                embeddedServer(
                    CIO,
                    port = 9090,
                    module = {
                        myApplicationModule(
                            onStart = {
                                scope.cancel()
                            },
                            onStop = {},
                        )
                    },
                    configure = {
                    },
                ).start(wait = true)
            }
        }
    }
}
