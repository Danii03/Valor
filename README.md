# Valor

Ein Minecraft-Update im Vanilla-Stil für **Fabric 1.21.1**. Neue Inhalte sollen sich anfühlen, als hätte Mojang sie selbst eingebaut, und mit dem bestehenden Spiel zusammenspielen.

## Befehle

| Befehl | Zweck |
|---|---|
| `./gradlew runClient` | Spiel mit der Mod starten |
| `./gradlew runDatagen` | Modelle, Blockstates, Loot Tables, Rezepte, Tags und Übersetzungen erzeugen |
| `./gradlew build` | Fertige Mod-JAR bauen (`build/libs/`) |

Nach jeder Änderung an einem Datagen-Provider: zuerst `runDatagen`, dann `runClient`.

## Projektstruktur

```
src/
├── main/                                 Code für Client UND Server
│   ├── java/net/valor/
│   │   ├── Valor.java                    Einstiegspunkt, startet alle Registries
│   │   ├── registry/                     ValorBlocks, ValorItems, ValorSounds, ValorCreativeTabs, …
│   │   ├── tag/                          Eigene Tag-Keys (ValorBlockTags, ValorItemTags)
│   │   ├── network/                      Netzwerkpakete Client ↔ Server
│   │   ├── platform/ClientHooks.java     Brücke, über die gemeinsamer Code Client-Code auslöst
│   │   ├── world/…                       Eigene Block-/Item-/Entity-Klassen (wie bei Vanilla)
│   │   └── mixin/                        Eingriffe in Vanilla-Code
│   ├── resources/                        Handgemachte Assets (Texturen, Sounds, eigene Modelle)
│   │   └── assets/valor/
│   │       ├── textures/{block,item,entity}/
│   │       ├── models/{block,item}/      nur eigene Modelle (z. B. aus Blockbench)
│   │       ├── particles/                Sprite-Zuordnung der Partikel
│   │       └── sounds.json
│   └── generated/                        Datagen-Ausgabe, NIE von Hand bearbeiten
└── client/                               Nur Client-Code
    └── java/net/valor/client/
        ├── ValorClient.java              Client-Einstiegspunkt
        ├── ValorRenderLayers.java        Transparenz (cutout/translucent) für Blöcke
        ├── gui/screens/                  Eigene Interfaces (z. B. EchoFluteScreen)
        ├── particle/                     Partikel-Klassen und -Effekte
        ├── renderer/                     Eigene Render-Logik (z. B. Echoflöte in der Ego-Perspektive)
        ├── mixin/                        Eingriffe in Vanilla-Client-Code
        ├── network/                      Empfang von Server-Paketen
        └── datagen/                      Ein Provider pro Datentyp
```

## So kommt neuer Inhalt hinein (Beispiel: neuer Block)

1. **Registrieren** in `registry/ValorBlocks.java`
2. **Textur** nach `assets/valor/textures/block/<name>.png`
3. **Modell/Blockstate** in `ValorModelProvider`
4. **Loot Table** in `ValorBlockLootTableProvider`
5. **Tags** in `ValorBlockTagProvider` (Werkzeug, Abbaustufe, …)
6. **Rezept** in `ValorRecipeProvider`
7. **Namen** in beiden Sprach-Providern (`en_us` + `de_de`)
8. **Creative-Platz** in `ValorCreativeTabs`, direkt neben verwandten Vanilla-Einträgen
9. `./gradlew runDatagen` und dann `./gradlew runClient`

## Weltgenerierung (Biome, Bäume, Features)

| Was | Wo |
|---|---|
| Biom-Schlüssel (`valor:whispering_woods`) | `world/level/biome/ValorBiomes.java` |
| Klima-Platz in der Overworld | `mixin/OverworldBiomeBuilderMixin.java` (meldet das Biom im Dark-Forest-Klima an) |
| **Seltenheit und Gebietsgröße** | `world/level/biome/WhisperingWoodsRegions.java` + `mixin/MultiNoiseBiomeSourceMixin.java` |
| **Waldzonen** (Outer Woods, Deepwood, Hidden Groves) | `world/level/levelgen/WhisperingWoodsZones.java`, angewendet über den Placement-Filter `valor:whispering_woods_zone` |
| Eigene Feature-Typen (Code) | `registry/ValorFeatures.java` → `TemplateTreeFeature` (NBT-Bäume), `StreamFeature` (Bäche) |
| Biom-Inhalt, Farben, Spawns, Musik | `client/datagen/worldgen/ValorBiomeData.java` |
| Was generiert wird | `client/datagen/worldgen/ValorConfiguredFeatures.java` |
| Wo/wie oft, pro Zone | `client/datagen/worldgen/ValorPlacedFeatures.java` (`zone(outer, deepwood, grove)`) |
| Biom-Tags (Strukturen, `is_forest`, …) | `client/datagen/ValorBiomeTagProvider.java` |
| Baum-Vorlagen (`.nbt`) | `resources/data/valor/structure/whispering_woods/` |

### Seltenheit und Größe der Whispering Woods

Zwei getrennte Stellschrauben:

- **Größe** ergibt sich aus dem Vanilla-Klima: Das Biom übernimmt immer den *ganzen* Dark-Forest-Klimabereich einer Region, nicht nur einen Streifen davon. Ein Gebiet ist also so groß wie ein vollständiger Dark Forest.
- **Seltenheit** steuert eine sehr großräumige Noise mit Welt-Seed (`WhisperingWoodsRegions`). Nur wo sie über `THRESHOLD` liegt, wird aus Dark Forest Flüsterwald (aktuell ca. 25 % des Dark-Forest-Klimas). Höherer `THRESHOLD` = seltener. `REGION_SCALE` bestimmt, wie weit die Gebiete auseinanderliegen.

Übergänge zu anderen Biomen bleiben die eines Dark Forest, das Terrain bleibt Vanilla.

### Waldzonen

Keine eigenen Biome, sondern weiche Gewichte innerhalb des Bioms (`WhisperingWoodsZones`):

- **Outer Woods**: Rand des Waldes, weniger dicht, kleine/mittlere Bäume, Vanilla-Fichten.
- **Deepwood**: wächst mit dem Abstand zum Biomrand, von einer großräumigen Noise moduliert. Riesenbäume, große Farne, dunklere Bodenakzente.
- **Hidden Groves**: kleine Lichtungen, wo eine feine Noise Spitzen hat. Kaum Bäume, viel hohes Gras, häufiger Bäche.

Jedes Feature macht `count` Versuche pro Chunk; jeder Versuch bleibt mit der Wahrscheinlichkeit seiner Zone erhalten.

### Atmosphäre (nur Client)

| Was | Wo |
|---|---|
| Steuerung, sanftes Ein-/Ausblenden („presence“) | `client/WhisperingWoodsAmbience.java` |
| Bodennebel (eigener Renderer, keine Partikel) | `client/renderer/ForestMistRenderer.java` – Dichte: `MAX_ALPHA`, `MAX_WISPS`, `SPAWN_RATE`; Farbe `#788F85` |
| Glitzerlichter | `client/particle/WhisperingWoodsParticles.java` (Häufigkeit: `GLIMMER_RATE`), `GlimmerParticle` (Helligkeit: `MAX_ALPHA`, `GLOW_ALPHA`, `GLOW_SIZE`) |
| Dunkles Laub der Bäume | Block `valor:whispering_leaves` (`ValorBlocks`), Färbung `ValorBlockColors.WHISPERING_LEAVES`; ersetzt beim Platzieren die Fichtenblätter der `.nbt`-Bäume (`TemplateTreeProcessor`), die Originaldateien bleiben unverändert |
| Vögel und Krähen (Zeitsteuerung) | `client/resources/sounds/WhisperingWoodsSoundHandler.java` |
| Sound-Clips | `resources/assets/valor/sounds/ambient/whispering_woods/` (je ~15 s, aus den Originalaufnahmen verlustfrei geschnitten) |

**Testen:** Biome und Features wirken nur in **neu generierten Chunks**, am besten in einer neuen Welt. Neue Welt → *Weltoptionen* → Welttyp **Einzelnes Biom** → *Flüsterwald* (Zonen-Ränder fehlen dort, alles ist Deepwood/Groves), oder in einer normalen neuen Welt `/locate biome valor:whispering_woods`.

## License

CC0. Siehe `LICENSE`.
