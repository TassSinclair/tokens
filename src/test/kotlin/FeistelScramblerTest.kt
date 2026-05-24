import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FeistelScramblerTest {

    private val domainSize = 32 * 32 * 32 * 32 * 32 * 32
    private val scrambler = FeistelScrambler(key = 0x2783dabL, domainSize = domainSize)

    @Test
    fun `scramble and unscramble roundtrip`() {
        for (id in 0..20) {
            assertEquals(id, scrambler.unscramble(scrambler.scramble(id)))
        }
    }

    @Test
    fun `first 100_000 scrambled values are unique`() {
        val seen = mutableSetOf<Int>()
        for (id in 0 until 100_000) {
            val scrambled = scrambler.scramble(id)
            assertTrue(seen.add(scrambled), "Collision at ID $id: $scrambled")
            assertEquals(id, scrambler.unscramble(scrambled), "Roundtrip failed for ID $id")
        }
    }

    @Test
    fun `scramble rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            scrambler.scramble(-1)
        }
    }

    @Test
    fun `scramble rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            scrambler.scramble(domainSize)
        }
    }

    @Test
    fun `unscramble rejects negative input`() {
        assertFailsWith<IllegalArgumentException> {
            scrambler.unscramble(-1)
        }
    }

    @Test
    fun `unscramble rejects input at domain size`() {
        assertFailsWith<IllegalArgumentException> {
            scrambler.unscramble(domainSize)
        }
    }
}
