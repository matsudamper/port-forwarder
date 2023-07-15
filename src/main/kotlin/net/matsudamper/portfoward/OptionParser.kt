package net.matsudamper.portfoward

class OptionParser(
    private val options: List<String>,
) {
    fun get(): Map<String, String> {
        return options.mapNotNull { text ->
            val result = Regex.matchEntire(text)
            return@mapNotNull if (result != null) {
                result.groupValues[1] to result.groupValues[2]
            } else {
                null
            }
        }.toMap()
    }

    companion object {
        private val Regex = """^--(.+?)=(.+?)$""".toRegex()
    }
}
