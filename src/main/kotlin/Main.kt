import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class Tokens : CliktCommand(help = "Generate and decode type-prefixed tokens") {
    override fun run() = Unit
}

class Encode : CliktCommand(
    help = "Generate a token from a numeric ID",
    epilog = """Examples:
    |```
    |  tokens encode --type u 42
    |  tokens encode --prefix U --length 6 --seed 0x2783DAB 42
    |```""".trimMargin(),
    printHelpOnEmptyArgs = true,
) {
    val id by argument(help = "Numeric ID to encode").long().optional()
    val type by option("--type", "-t", help = "Token type name from config")
    val prefix by option("--prefix", "-p", help = "Token prefix (e.g. U, INV)")
    val length by option("--length", "-l", help = "Encoded length in Base32 characters").int()
    val seed by option("--seed", "-s", help = "Cipher seed (decimal or 0x hex)").convert("SEED") { parseSeed(it) }
    val configPath by option("--config", "-c", help = "Path to config file").path()

    override fun run() {
        val resolvedId = id ?: throw PrintHelpMessage(currentContext, error = true)
        val tc = resolveType(type, prefix, length, seed, configPath)
        try {
            val token = DynamicToken(tc.prefix, tc.length, tc.seed, resolvedId)
            echo(token.value)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage("Error: ${e.message}", statusCode = 1)
        }
    }
}

class Decode : CliktCommand(help = "Recover the numeric ID from a token string") {
    val token by argument(help = "Token to decode (e.g. U_71VHDHS)")
    val type by option("--type", "-t", help = "Token type name from config")
    val prefix by option("--prefix", "-p", help = "Token prefix")
    val length by option("--length", "-l", help = "Encoded length in Base32 characters").int()
    val seed by option("--seed", "-s", help = "Cipher seed (decimal or 0x hex)").convert("SEED") { parseSeed(it) }
    val configPath by option("--config", "-c", help = "Path to config file").path()

    override fun run() {
        val tc = resolveTypeForDecode(token, type, prefix, length, seed, configPath)
        try {
            val decoded = DynamicToken(tc.prefix, tc.length, tc.seed, token)
            echo(decoded.toId())
        } catch (e: IllegalArgumentException) {
            throw PrintMessage("Error: ${e.message}", statusCode = 1)
        }
    }
}

class ListTypes : CliktCommand(name = "list", help = "List configured token types") {
    val configPath by option("--config", "-c", help = "Path to config file").path()

    override fun run() {
        val config = loadConfig(configPath)
            ?: throw PrintMessage("No config file found. Expected ~/.config/tokens.toml", statusCode = 1)
        if (config.types.isEmpty()) {
            echo("No token types configured.")
            return
        }
        for ((name, tc) in config.types.entries.sortedBy { it.key }) {
            echo("  %-8s prefix=%-4s length=%-3d seed=0x%s".format(name, tc.prefix, tc.length, tc.seed.toString(16)))
        }
    }
}

private fun parseSeed(s: String): Long =
    if (s.startsWith("0x", ignoreCase = true)) s.drop(2).toLong(16) else s.toLong()

private fun loadConfig(explicit: Path?): TokenConfig? {
    if (explicit != null) return TokenConfig.load(explicit)
    val home = System.getProperty("user.home")
    return TokenConfig.loadFrom(
        Path.of(home, ".config", "tokens.toml"),
    )
}

private fun resolveType(
    type: String?, prefix: String?, length: Int?, seed: Long?, configPath: Path?
): TokenTypeConfig {
    if (type != null) {
        val config = loadConfig(configPath)
            ?: throw PrintMessage("No config file found", statusCode = 1)
        return config.types[type]
            ?: throw PrintMessage(
                "Unknown type '$type'. Available: ${config.types.keys.sorted().joinToString()}",
                statusCode = 1,
            )
    }
    if (prefix != null && length != null && seed != null) {
        return TokenTypeConfig(name = "inline", prefix = prefix, length = length, seed = seed)
    }
    throw PrintMessage("Provide --type or all of --prefix, --length, --seed", statusCode = 1)
}

private fun resolveTypeForDecode(
    tokenValue: String, type: String?, prefix: String?, length: Int?, seed: Long?, configPath: Path?
): TokenTypeConfig {
    if (type != null) {
        val config = loadConfig(configPath)
            ?: throw PrintMessage("No config file found", statusCode = 1)
        return config.types[type]
            ?: throw PrintMessage("Unknown type '$type'", statusCode = 1)
    }
    if (prefix != null && length != null && seed != null) {
        return TokenTypeConfig(name = "inline", prefix = prefix, length = length, seed = seed)
    }
    // Auto-detect from config by matching the token's prefix
    val config = loadConfig(configPath)
    if (config != null) {
        val match = config.findByPrefix(tokenValue)
        if (match != null) return match
    }
    throw PrintMessage(
        "Cannot determine token type. Provide --type, all of --prefix/--length/--seed, or configure a matching prefix in tokens.toml",
        statusCode = 1,
    )
}

fun main(args: Array<String>) = Tokens()
    .subcommands(Encode(), Decode(), ListTypes())
    .main(args)
