# tokens

Turn sequential database IDs into short, opaque, type-prefixed tokens — without a lookup table.

```
ID 0  → U_SH5JWN
ID 1  → U_GKDM6Q
ID 2  → U_0528VT
```

Each token type gets its own prefix and seed, so the same integer ID produces different tokens for different entity types:

```kotlin
UserToken(1).value    // "U_GKDM6Q"
TenantToken(1).value  // "T_RJ4NCA"
```

Tokens are reversible — you can always recover the original ID:

```kotlin
val token = UserToken(42)
token.toId()  // 42
```

## How it works

1. A **Feistel scrambler** permutes integers reversibly within a fixed domain, so that sequential inputs produce unique, unrelated outputs.

2. A **Base32 encoder** encodes each integer as a short, human-friendly 6-character string using a seed-specific shuffled alphabet (Crockford Base32, minus ambiguous characters like `I`, `L`, `O`) to prevent ID guessing and enumeration.

3. A **typed token wrapper** pairs a type prefix (e.g. `U_`, `T_`) with an encoded ID, so tokens are self-describing and can't be mixed across entity types.

## Defining new token types

```kotlin
class ProjectToken : Token {
    constructor(value: String) : super(PREFIX, B32, value)
    constructor(id: Int) : super(PREFIX, B32, id)

    companion object {
        const val PREFIX = 'P'
        val B32 = Base32Crockford(0xYOUR_SEED_HERE)
    }
}
```

Use a different seed for each token type to ensure their output spaces don't collide.

## Running tests

```
./gradlew test
```
