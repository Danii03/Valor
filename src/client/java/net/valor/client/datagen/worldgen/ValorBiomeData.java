package net.valor.client.datagen.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.valor.world.level.biome.ValorBiomes;

/**
 * Valor's biomes. Counterpart to vanilla's {@code OverworldBiomes}.
 * <p>
 * <b>Feature order matters:</b> within a generation step, vanilla features must appear in the same
 * relative order in every biome, or the game refuses to load the world ("Feature order cycle").
 * The vanilla features below are therefore a subset of Old Growth Spruce Taiga's list, in its
 * order. Valor's own features can go anywhere.
 */
public final class ValorBiomeData {

	// Whispering Woods colors: noticeably more muted and darker than vanilla forests, but readable by day.
	// Grass also tints short/tall grass and ferns. Spruce-type leaves ignore the foliage color; the trees here
	// use Whispering Leaves, which carry their own tint (see ValorBlockColors).
	private static final int WHISPERING_WOODS_GRASS = 0x304B2C;
	private static final int WHISPERING_WOODS_FOLIAGE = 0x2F4B36;
	private static final int WHISPERING_WOODS_WATER = 0x4B6E73;
	private static final int WHISPERING_WOODS_WATER_FOG = 0x2E4448;
	private static final int WHISPERING_WOODS_FOG = 0x8EA39B;
	private static final int WHISPERING_WOODS_SKY = 0x7088A6;

	private ValorBiomeData() {}

	public static void bootstrap(BootstrapContext<Biome> context) {
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
		context.register(ValorBiomes.WHISPERING_WOODS, whisperingWoods(placed, carvers));
	}

	/**
	 * Ancient, dense forest: hand-built spruce giants, dark grass with ferns and tall grass, small streams.
	 * <p>
	 * Ambience: birds and crows, ground mist and glimmers are client-side effects
	 * ({@code WhisperingWoodsAmbience}), since they need fades, day/night and placement near the ground.
	 * A quiet wind/leaves bed is not included: vanilla has no fitting Overworld loop (its ambient loops are
	 * Nether biomes). Once there is a recording, register a sound event in {@code ValorSounds} and add it
	 * here via {@code .ambientLoopSound(sound)}; vanilla then loops it in this biome and fades it in and out
	 * at the border by itself.
	 */
	private static Biome whisperingWoods(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(spawns);
		spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 8, 4, 4));
		spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
		spawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
		BiomeDefaultFeatures.commonSpawns(spawns);

		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placed, carvers);
		// Same as vanilla's (private) OverworldBiomes.globalOverworldGeneration.
		BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
		BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
		BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
		BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
		BiomeDefaultFeatures.addDefaultSprings(generation);
		BiomeDefaultFeatures.addSurfaceFreezing(generation);

		// Streams: mostly in the Hidden Groves.
		generation.addFeature(GenerationStep.Decoration.LAKES, ValorPlacedFeatures.STREAM_WHISPERING_WOODS);

		// Ground: mostly grass. Podzol, coarse dirt and rooted dirt only as small accents.
		BiomeDefaultFeatures.addMossyStoneBlock(generation);
		generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ValorPlacedFeatures.DISK_PODZOL);
		generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ValorPlacedFeatures.DISK_COARSE_DIRT);
		generation.addFeature(GenerationStep.Decoration.LOCAL_MODIFICATIONS, ValorPlacedFeatures.DISK_ROOTED_DIRT);

		BiomeDefaultFeatures.addDefaultOres(generation);
		// Clay and gravel from addDefaultSoftDisks, without the sand (no beaches along forest streams).
		generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
		generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_GRAVEL);

		// Undergrowth first, trees last, so it also covers the ground under the canopies.
		// Densities per forest zone (Outer Woods, Deepwood, Hidden Groves) are set in ValorPlacedFeatures.
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.MOSS_PATCH_WHISPERING_WOODS);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.PATCH_LARGE_FERN_WHISPERING_WOODS);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.PATCH_TALL_GRASS_WHISPERING_WOODS);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.PATCH_GRASS_WHISPERING_WOODS);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.BROWN_MUSHROOM_WHISPERING_WOODS);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.RED_MUSHROOM_WHISPERING_WOODS);
		BiomeDefaultFeatures.addDefaultExtraVegetation(generation);
		BiomeDefaultFeatures.addCommonBerryBushes(generation);
		// Biggest trees first, vanilla spruces last as gap fillers.
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.WHISPERING_TREE_3);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.WHISPERING_TREE_1);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.WHISPERING_TREE_4);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.WHISPERING_TREE_2);
		generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ValorPlacedFeatures.SPRUCE_WHISPERING_WOODS);

		BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
				.grassColorOverride(WHISPERING_WOODS_GRASS)
				.foliageColorOverride(WHISPERING_WOODS_FOLIAGE)
				.waterColor(WHISPERING_WOODS_WATER)
				.waterFogColor(WHISPERING_WOODS_WATER_FOG)
				.fogColor(WHISPERING_WOODS_FOG)
				.skyColor(WHISPERING_WOODS_SKY)
				.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
				.backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST))
				.build();

		return new Biome.BiomeBuilder()
				.hasPrecipitation(true)
				.temperature(0.6F)
				.downfall(0.85F)
				.specialEffects(effects)
				.mobSpawnSettings(spawns.build())
				.generationSettings(generation.build())
				.build();
	}
}
