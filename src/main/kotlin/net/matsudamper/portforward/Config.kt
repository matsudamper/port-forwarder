package net.matsudamper.portforward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("destination") val destination: Destination,
    @SerialName("key") val key: Key,
    @SerialName("forward") val forward: Map<String, String>,
) {
    @Serializable
    data class Destination(
        @SerialName("text") val text: String? = null,
        @SerialName("command") val command: String? = null,
    )

    @Serializable
    data class Key(
        @SerialName("text") val text: String? = null,
        @SerialName("command") val command: String? = null,
    )
}
