import org.tomlj.Toml
import java.nio.file.Path

data class TokenTypeConfig(
    val name: String,
    val prefix: String,
    val length: Int,
    val seed: Long,
)

class TokenConfig(val types: Map<String, TokenTypeConfig>) {

    /** Finds the configured type whose prefix matches the start of [tokenValue], preferring the longest match. */
    fun findByPrefix(tokenValue: String): TokenTypeConfig? =
        types.values
            .filter { tokenValue.startsWith("${it.prefix}_") }
            .maxByOrNull { it.prefix.length }

    companion object {
        /** Loads config from the first existing path, or returns null if none exist. */
        fun loadFrom(vararg paths: Path): TokenConfig? =
            paths.firstOrNull { it.toFile().exists() }?.let { load(it) }

        fun load(path: Path): TokenConfig {
            val result = Toml.parse(path)
            if (result.hasErrors()) {
                val msgs = result.errors().joinToString("\n") { "  ${it.position()}: ${it.message}" }
                throw IllegalArgumentException("Failed to parse $path:\n$msgs")
            }
            val types = mutableMapOf<String, TokenTypeConfig>()
            for (key in result.keySet()) {
                if (!result.isTable(key)) continue
                val table = result.getTable(key)!!
                val prefix = table.getString("prefix")
                    ?: throw IllegalArgumentException("[$key]: missing 'prefix'")
                val length = table.getLong("length")?.toInt()
                    ?: throw IllegalArgumentException("[$key]: missing 'length'")
                val seed = table.getLong("seed")
                    ?: throw IllegalArgumentException("[$key]: missing 'seed'")
                types[key] = TokenTypeConfig(name = key, prefix = prefix, length = length, seed = seed)
            }
            return TokenConfig(types)
        }
    }
}
