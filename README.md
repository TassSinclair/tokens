# Tokens

Convert sequential database IDs into compact, non-enumerable, type-prefixed tokens.

```
User ID 1  → U_71VHDH
User ID 2  → U_1MXBJ1
User ID 3  → U_ZR5KA5
```

Each token type has its own prefix, length, and seed, so the same integer ID produces different tokens for different entity types:

```kotlin
UserToken(1).value     // "U_71VHDH"     (6-char)
InvoiceToken(1).value  // "INV_0RXG6MSN" (8-char)
```

Tokens are reversible, you can always recover the original ID:

```kotlin
UserToken(42).toId()  // 42
```

## How it works

1. A **Feistel cipher** permutes integers reversibly within a fixed domain, so that sequential inputs produce unique, unrelated outputs.

2. A **Base32 encoder** encodes each integer as a short, human-friendly string using [Crockford's Base32](https://www.crockford.com/base32.html) (minus ambiguous characters like `I`, `L`, `O`). The encoded length is configurable per token type (1-12 characters).

3. A **typed token wrapper** pairs a type prefix (e.g. `U_`, `INV_`) with an encoded ID, so tokens are self-describing and can't be mixed across entity types.

## Input canonicalisation

Tokens are permissive of common transcription errors. When constructed from a string, confusable characters are automatically replaced with their canonical equivalents following [Crockford's Base32](https://www.crockford.com/base32.html) rules, and lowercase is normalised to uppercase:

| Input        | Canonical |
|--------------|-----------|
| `O`, `o`     | `0`       |
| `I`, `i`, `L`, `l` | `1` |
| `U`, `u`     | `V`       |
| `a`–`z`      | `A`–`Z`   |

```kotlin
UserToken("U_abcol2").value  // "U_ABC012"
UserToken("U_abcol2").toId() == UserToken("U_ABC012").toId()  // true
```

## CLI

Generate and decode tokens from the command line.

### Setup

```
./gradlew installDist
cp tokens.example.toml tokens.toml   # edit seeds as needed
```

The binary is at `./build/install/tokens/bin/tokens`.

### Config file

The CLI looks for config at `./tokens.toml`, then `~/.config/tokens.toml`. Each section defines a token type:

```toml
[u]
prefix = "U"
length = 6
seed = 0x2783DAB

[inv]
prefix = "INV"
length = 8
seed = 0x3B4FF74
```

### Usage

```bash
# Encode using a configured type
tokens encode --type u --id 42        # U_KH5HCY8

# Encode with explicit parameters (no config needed)
tokens encode --prefix U --length 6 --seed 0x2783DAB --id 42

# Decode (auto-detects type from prefix)
tokens decode U_KH5HCY8              # 42

# List configured types
tokens list
```

## Defining new token types (library)

```kotlin
class ProjectToken : Token {
    constructor(value: String) : super(PREFIX, LENGTH, SEED, value)
    constructor(id: Long) : super(PREFIX, LENGTH, SEED, id)

    companion object {
        const val PREFIX = "P"
        const val LENGTH = 6
        const val SEED = 0xYOUR_SEED_HEREL
    }
}
```

Use a different seed for each token type to ensure their output spaces don't collide. Length can be between 1 and 12 characters.

## Running tests

```
./gradlew test
```

## Sources
- [Base32 (crockford.com)](https://www.crockford.com/base32.html)
- [Feistel cipher (asecuritysite.com)](https://asecuritysite.com/fpe/fei)
