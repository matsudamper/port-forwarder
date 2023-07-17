package net.matsudamper.portforward

import org.junit.jupiter.api.Test

class OptionParserTest {
    @Test
    fun noValueTest() {
        val debug = OptionParser(
            listOf("--debug"),
        ).get()["debug"]

        if (debug != "") {
            throw AssertionError(
                "[$debug] isNot Empty",
            )
        }
    }

    @Test
    fun parseTest() {
        val config = OptionParser(
            listOf("--config=config.yml"),
        ).get()["config"]

        if (config != "config.yml") {
            throw AssertionError(
                "[$config] isNot config.yml",
            )
        }
    }
}
