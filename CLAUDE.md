# Valor – Working Notes

Vanilla-style content mod for Fabric 1.21.1 with **Mojang mappings** (`ResourceLocation`, `BlockBehaviour.Properties`, `RecipeOutput`, …). Java 21. User communicates in German; code, comments and identifiers are English.

## Design principles
- **Feels like Mojang made it.** Mirror vanilla patterns, naming and balancing. Integrate through vanilla systems (vanilla tags, loot tables, creative tabs, advancements, villager trades, worldgen) instead of parallel mod systems.
- **No custom creative tab.** Insert entries into vanilla tabs next to related vanilla items (`ValorCreativeTabs`).
- **Datagen first.** Block states, models, loot, recipes, tags and lang go through `src/client/java/net/valor/client/datagen`. `src/main/generated` is output only. Hand-made assets (textures, Blockbench models, sounds.json) live in `src/main/resources`.
- Every player-facing string must exist in **both** `en_us` and `de_de`. German follows the official Minecraft translation style.
- Prefer Fabric API events over mixins; mixins are the last resort. Keep each one small and documented.

## Conventions
- Package root `net.valor` (client: `net.valor.client`). Mod ID `valor`; use `Valor.id("path")`.
- Registry holders: `registry/Valor<Thing>s` (final class, static fields, private `register` helpers, `init()` called from `Valor.onInitialize` in dependency order).
- Custom gameplay classes mirror vanilla package layout: `world/level/block`, `world/item`, `world/entity`, `world/level/block/entity`, …
- Client-only code (renderers, models, screens, particles) goes in the `client` source set only. Screens live in `net.valor.client.gui.screens`.
- Common code that must trigger client behavior (e.g. an item opening a screen) calls `net.valor.platform.ClientHooks.get()`; add a default no-op method to `ClientHooks.Handler` and implement it in `net.valor.client.ValorClientHooks`.
- Data shared by client and server (e.g. `EchoFluteNote` colors/pitches) belongs in common code.
- Networking: payload records in `net.valor.network` named vanilla-style (`Serverbound…Payload` / `Clientbound…Payload`), registered in `ValorNetworking`; client receivers in `net.valor.client.network.ValorClientNetworking`. Validate and rate-limit (`PlayerRateLimiter`) every serverbound packet.
- Effects triggered by a player: the acting client plays them instantly; the server relays to *other* players only (`Level.playSound(player, …)` excludes the player, S2C payloads skip them) – never double-spawn.
- Particles: types in `registry/ValorParticleTypes`, sprite JSON in `assets/valor/particles/`, client classes + providers in `net.valor.client.particle`. Prefer vanilla sprites and `ColorParticleOption` for tinting.
- Registry names: lowercase snake_case, vanilla-style (`ruby_block`, not `block_ruby`).
- Tabs for indentation (see `.editorconfig`).

## Verify
`./gradlew build`, and `./gradlew runDatagen` as a **separate** invocation (running both in one Gradle call fails on task ordering).
