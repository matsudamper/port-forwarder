package net.matsudamper.portfoward

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.config.keys.ClientIdentityLoader
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.session.SessionContext
import org.apache.sshd.common.util.net.SshdSocketAddress
import java.net.InetSocketAddress
import java.security.KeyPair
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class Forward(
    val localHost: String,
    val localPort: Int,
    val serverHost: String,
    val serverPort: Int,
    private val keyPath: String?,
    private val destination: String,
) {
    private val sshClient = SshClient.setUpDefaultClient()
        .also { it.start() }
    val input: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

    private val job = Job()
    suspend fun start() {
        println("$destination start Forward: $localPort -> $serverHost:$serverPort")

        withContext(Dispatchers.Default + job) {
            while (isActive) {
                sshClient.clientIdentityLoader = object : ClientIdentityLoader by ClientIdentityLoader.DEFAULT {
                    override fun loadClientIdentities(session: SessionContext?, location: NamedResource?, provider: FilePasswordProvider?): MutableIterable<KeyPair> {
                        return if (keyPath != null) {
                            val resolvedPath = keyPath.replaceFirst("^~".toRegex(), System.getProperty("user.home"))
                            ClientIdentityLoader.DEFAULT.loadClientIdentities(session, { resolvedPath }, provider)
                        } else {
                            ClientIdentityLoader.DEFAULT.loadClientIdentities(session, location, provider)
                        }
                    }
                }
                sshClient.connect(destination)
                    .verify(1.seconds.toJavaDuration())
                    .session.use { session ->
                        val channel = session.createLocalPortForwardingTracker(
                            SshdSocketAddress.toSshdSocketAddress(
                                InetSocketAddress.createUnresolved(localHost, localPort),
                            ),
                            SshdSocketAddress.toSshdSocketAddress(
                                InetSocketAddress.createUnresolved(serverHost, serverPort),
                            ),
                        )

                        if (session.auth().verify().isSuccess) {
                            println("$serverHost:$serverPort auth success")
                            channel.session.waitFor(listOf(ClientSession.ClientSessionEvent.CLOSED), 0)
                        } else {
                            throw IllegalStateException("auth failed")
                        }
                    }
            }
        }
    }

    fun kill() {
        job.cancel()
        sshClient.stop()
    }

    suspend fun collectText() {
    }
}
