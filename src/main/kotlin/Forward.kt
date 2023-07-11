import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.util.net.SshdSocketAddress
import java.lang.IllegalStateException
import java.net.InetSocketAddress

class Forward(
        val localPort: Int,
        val serverHost: String,
        val serverPort: Int,
        private val keyPath: String,
        private val destination: String,
) {
    private val sshClient = SshClient.setUpDefaultClient()
            .also { it.start() }
    val input: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

    suspend fun start() {
        println("$destination start Forward: $localPort -> $serverHost:$serverPort")

        withContext(Dispatchers.Default) {
            while (isActive) {
                sshClient.connect(destination).verify().session.use { session ->
                    val channel = session.createLocalPortForwardingTracker(
                            SshdSocketAddress.toSshdSocketAddress(
                                    InetSocketAddress.createUnresolved("127.0.0.1", localPort)
                            ),
                            SshdSocketAddress.toSshdSocketAddress(
                                    InetSocketAddress.createUnresolved(serverHost, serverPort)
                            ),
                    )

                    if (session.auth().verify().isSuccess) {
                        channel.session.waitFor(listOf(ClientSession.ClientSessionEvent.CLOSED), 0)
                    } else {
                        throw IllegalStateException("auth failed")
                    }
                }
            }
        }
    }

    fun waitFor() {

    }

    fun kill() {
        sshClient.stop()
    }

    suspend fun collectText() {
    }
}
