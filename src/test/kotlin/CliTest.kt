import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CliTest {

    private val configFile = File.createTempFile("tokens", ".toml").apply {
        deleteOnExit()
        writeText("""
            [u]
            prefix = "U"
            length = 6
            seed = 0x2783DAB

            [inv]
            prefix = "INV"
            length = 8
            seed = 0x3B4FF74
        """.trimIndent())
    }

    private fun cli() = Tokens().subcommands(Encode(), Decode(), ListTypes())

    private fun configArgs() = listOf("--config", configFile.absolutePath)

    // --- Encode with --type from config ---

    @Test
    fun `encode with type from config`() {
        val result = cli().test("encode --type u -c ${configFile.absolutePath} 42")
        val expected = DynamicToken("U", 6, 0x2783DABL, 42L).value
        assertEquals(0, result.statusCode)
        assertEquals(expected, result.stdout.trim())
    }

    @Test
    fun `encode invoice type from config`() {
        val result = cli().test("encode --type inv -c ${configFile.absolutePath} 1")
        val expected = DynamicToken("INV", 8, 0x3B4FF74L, 1L).value
        assertEquals(0, result.statusCode)
        assertEquals(expected, result.stdout.trim())
    }

    // --- Encode with explicit params ---

    @Test
    fun `encode with explicit prefix length seed`() {
        val result = cli().test("encode --prefix U --length 6 --seed 0x2783DAB 42")
        val expected = DynamicToken("U", 6, 0x2783DABL, 42L).value
        assertEquals(0, result.statusCode)
        assertEquals(expected, result.stdout.trim())
    }

    @Test
    fun `encode explicit params match config type`() {
        val fromType = cli().test("encode --type u -c ${configFile.absolutePath} 99")
        val fromExplicit = cli().test("encode --prefix U --length 6 --seed 0x2783DAB 99")
        assertEquals(fromType.stdout, fromExplicit.stdout)
    }

    @Test
    fun `encode with decimal seed`() {
        val decimalSeed = 0x2783DABL
        val result = cli().test("encode --prefix U --length 6 --seed $decimalSeed 42")
        val expected = DynamicToken("U", 6, decimalSeed, 42L).value
        assertEquals(0, result.statusCode)
        assertEquals(expected, result.stdout.trim())
    }

    @Test
    fun `encode id 0 works`() {
        val result = cli().test("encode --prefix U --length 6 --seed 0x2783DAB 0")
        val expected = DynamicToken("U", 6, 0x2783DABL, 0L).value
        assertEquals(0, result.statusCode)
        assertEquals(expected, result.stdout.trim())
    }

    // --- Encode then decode roundtrip ---

    @Test
    fun `encode then decode roundtrips via config`() {
        val encoded = cli().test("encode --type u -c ${configFile.absolutePath} 42").stdout.trim()
        val decoded = cli().test("decode -c ${configFile.absolutePath} $encoded").stdout.trim()
        assertEquals("42", decoded)
    }

    @Test
    fun `encode then decode roundtrips via explicit params`() {
        val encoded = cli().test("encode --prefix X --length 6 --seed 0xABCDEF 100").stdout.trim()
        val decoded = cli().test("decode --prefix X --length 6 --seed 0xABCDEF $encoded").stdout.trim()
        assertEquals("100", decoded)
    }

    // --- Decode auto-detects type from prefix ---

    @Test
    fun `decode auto-detects type from prefix`() {
        val token = DynamicToken("U", 6, 0x2783DABL, 7L)
        val result = cli().test("decode -c ${configFile.absolutePath} ${token.value}")
        assertEquals(0, result.statusCode)
        assertEquals("7", result.stdout.trim())
    }

    @Test
    fun `decode auto-detects longer prefix`() {
        val token = DynamicToken("INV", 8, 0x3B4FF74L, 123L)
        val result = cli().test("decode -c ${configFile.absolutePath} ${token.value}")
        assertEquals(0, result.statusCode)
        assertEquals("123", result.stdout.trim())
    }

    // --- Error cases ---

    @Test
    fun `encode with no args shows help`() {
        val result = cli().test("encode")
        assertContains(result.output, "Usage:")
        assertContains(result.output, "--type")
        assertContains(result.output, "--prefix")
        assertContains(result.output, "Examples:")
    }

    @Test
    fun `encode with type but no id shows help`() {
        val result = cli().test("encode --type u")
        assertContains(result.output, "Usage:")
        assertContains(result.output, "Examples:")
    }

    @Test
    fun `encode with unknown type fails`() {
        val result = cli().test("encode --type nope -c ${configFile.absolutePath} 1")
        assertTrue(result.statusCode != 0)
        assertContains(result.output, "Unknown type 'nope'")
    }

    @Test
    fun `encode with partial explicit params fails`() {
        val result = cli().test("encode --prefix X --length 6 1")
        assertTrue(result.statusCode != 0)
        assertContains(result.output, "--prefix, --length, --seed")
    }

    @Test
    fun `decode with unrecognised prefix fails`() {
        val emptyConfig = File.createTempFile("tokens-empty", ".toml").apply {
            deleteOnExit()
            writeText("")
        }
        val result = cli().test("decode -c ${emptyConfig.absolutePath} ZZZ_ABCDEF0")
        assertTrue(result.statusCode != 0)
    }

    @Test
    fun `encode with negative id fails`() {
        val result = cli().test("encode --prefix U --length 6 --seed 0x2783DAB -- -1")
        assertTrue(result.statusCode != 0)
    }

    // --- List ---

    @Test
    fun `list shows configured types`() {
        val result = cli().test("list -c ${configFile.absolutePath}")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "prefix=U")
        assertContains(result.stdout, "prefix=INV")
    }
}
