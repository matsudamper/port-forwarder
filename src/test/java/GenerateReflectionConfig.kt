import kotlinx.coroutines.runBlocking
import net.matsudamper.portfoward.Forward
import net.matsudamper.portfoward.main
import org.junit.jupiter.api.Test

class GenerateReflectionConfig {
    @Test
    fun generate() = runBlocking {
        main(
            arrayOf(
                "--port=8080",
                "--config=config.yml",
            ),
        )
    }
}