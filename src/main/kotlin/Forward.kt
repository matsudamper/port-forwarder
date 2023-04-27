import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class Forward(
    val localPort: Int,
    val serverHost: String,
    val serverPort: Int,
    private val keyPath: String,
    private val destination: String,
) {
    var process: Process? = null
    val input: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    val error: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

    fun start() {
        println("start Forward: $localPort -> $serverHost:$serverPort")
        val command = listOf(
            "ssh", "-i", keyPath,
            "-L", "$localPort:$serverHost:$serverPort",
            "-N",
            destination
        )
        println(command.joinToString(" "))
        val process = ProcessBuilder(command).start()
        this.process = process
    }

    fun waitFor() {
        process!!.waitFor()
    }

    fun kill() {
        process!!.destroy()
    }

    suspend fun collectText() {
        withContext(Dispatchers.IO) {
            launch {
                process!!.inputStream.bufferedReader()
                    .lineSequence()
                    .map { line ->
                        input.update {
                            it.toMutableList().also { mutable ->
                                mutable.add(line)
                            }
                        }
                    }
                    .toList()
            }
            launch {
                process!!.errorStream.bufferedReader()
                    .lineSequence()
                    .map { line ->
                        error.update {
                            it.toMutableList().also { mutable ->
                                mutable.add(line)
                            }
                        }
                    }
                    .toList()
            }
        }
    }
}
