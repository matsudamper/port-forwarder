import kotlinx.coroutines.runBlocking
import net.matsudamper.portforward.main
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