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

## License

CC0. Siehe `LICENSE`.
