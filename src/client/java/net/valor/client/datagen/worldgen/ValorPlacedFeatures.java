package net.valor.client.datagen.worldgen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.valor.Valor;
import net.valor.world.level.levelgen.placement.WhisperingWoodsZoneFilter;

/**
 * Placed features (where and how often). Counterpart to vanilla's {@code VegetationPlacements},
 * {@code TreePlacements}, … in {@code net.minecraft.data.worldgen.placement}.
 * <p>
 * <b>Forest zones.</b> Every Whispering Woods feature ends with a {@link WhisperingWoodsZoneFilter}:
 * a feature makes {@code count} attempts per chunk, and each attempt survives with a chance that
 * depends on the zone at its position, written below as {@code zone(outer woods, deepwood, hidden grove)}.
 * The zones blend into each other, so the forest gets denser towards its heart and opens up in
 * small clearings without visible borders.
 */
public final class ValorPlacedFeatures {

	// Trees, placed biggest first so the giants claim their space.
	public static final ResourceKey<PlacedFeature> WHISPERING_TREE_3 = key("whispering_tree_3");
	public static final ResourceKey<PlacedFeature> WHISPERING_TREE_1 = key("whispering_tree_1");
	public static final ResourceKey<PlacedFeature> WHISPERING_TREE_4 = key("whispering_tree_4");
	public static final ResourceKey<PlacedFeature> WHISPERING_TREE_2 = key("whispering_tree_2");
	public static final ResourceKey<PlacedFeature> SPRUCE_WHISPERING_WOODS = key("spruce_whispering_woods");

	// Undergrowth.
	public static final ResourceKey<PlacedFeature> PATCH_GRASS_WHISPERING_WOODS = key("patch_grass_whispering_woods");
	public static final ResourceKey<PlacedFeature> PATCH_LARGE_FERN_WHISPERING_WOODS = key("patch_large_fern_whispering_woods");
	public static final ResourceKey<PlacedFeature> PATCH_TALL_GRASS_WHISPERING_WOODS = key("patch_tall_grass_whispering_woods");
	public static final ResourceKey<PlacedFeature> BROWN_MUSHROOM_WHISPERING_WOODS = key("brown_mushroom_whispering_woods");
	public static final ResourceKey<PlacedFeature> RED_MUSHROOM_WHISPERING_WOODS = key("red_mushroom_whispering_woods");
	public static final ResourceKey<PlacedFeature> MOSS_PATCH_WHISPERING_WOODS = key("moss_patch_whispering_woods");

	// Ground and water.
	public static final ResourceKey<PlacedFeature> STREAM_WHISPERING_WOODS = key("stream_whispering_woods");
	public static final ResourceKey<PlacedFeature> DISK_COARSE_DIRT = key("disk_coarse_dirt");
	public static final ResourceKey<PlacedFeature> DISK_PODZOL = key("disk_podzol");
	public static final ResourceKey<PlacedFeature> DISK_ROOTED_DIRT = key("disk_rooted_dirt");

	private ValorPlacedFeatures() {}

	public static void bootstrap(BootstrapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		// ---- Trees. Many attempts fail on purpose (no room for the trunk, too steep), so counts are generous.
		// Giants dominate the Deepwood and are rare elsewhere.
		register(context, configured, WHISPERING_TREE_3, ValorConfiguredFeatures.WHISPERING_TREE_3,
				tree(3, zone(0.06F, 0.8F, 0.0F)));
		// Medium trees everywhere except in groves; densest in the Deepwood.
		register(context, configured, WHISPERING_TREE_1, ValorConfiguredFeatures.WHISPERING_TREE_1,
				tree(9, zone(0.4F, 0.9F, 0.06F)));
		// Leaning trees need a slope or water, so groves with streams get their share.
		register(context, configured, WHISPERING_TREE_4, ValorConfiguredFeatures.WHISPERING_TREE_4,
				tree(2, zone(0.4F, 0.4F, 0.7F)));
		// Small trees mostly in the Outer Woods.
		register(context, configured, WHISPERING_TREE_2, ValorConfiguredFeatures.WHISPERING_TREE_2,
				tree(6, zone(0.75F, 0.35F, 0.12F)));
		// Small spruces (vanilla shape, Whispering Leaves) fill gaps at the edge; the sapling check keeps them on open soil, never on canopies.
		List<PlacementModifier> spruce = new ArrayList<>(tree(4, zone(0.6F, 0.15F, 0.1F)));
		spruce.add(PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
		register(context, configured, SPRUCE_WHISPERING_WOODS, ValorConfiguredFeatures.WHISPERING_SPRUCE, spruce);

		// ---- Undergrowth: grass and ferns everywhere, large ferns rule the Deepwood, tall grass the groves.
		register(context, configured, PATCH_GRASS_WHISPERING_WOODS, VegetationFeatures.PATCH_TAIGA_GRASS,
				surface(CountPlacement.of(6), zone(0.9F, 0.7F, 1.0F)));
		register(context, configured, PATCH_LARGE_FERN_WHISPERING_WOODS, VegetationFeatures.PATCH_LARGE_FERN,
				surface(CountPlacement.of(3), zone(0.45F, 1.0F, 0.6F)));
		register(context, configured, PATCH_TALL_GRASS_WHISPERING_WOODS, VegetationFeatures.PATCH_TALL_GRASS,
				surface(CountPlacement.of(2), zone(0.7F, 0.4F, 1.0F)));
		register(context, configured, BROWN_MUSHROOM_WHISPERING_WOODS, VegetationFeatures.PATCH_BROWN_MUSHROOM,
				surface(RarityFilter.onAverageOnceEvery(3), zone(0.4F, 1.0F, 0.2F)));
		register(context, configured, RED_MUSHROOM_WHISPERING_WOODS, VegetationFeatures.PATCH_RED_MUSHROOM,
				surface(RarityFilter.onAverageOnceEvery(6), zone(0.4F, 1.0F, 0.2F)));
		register(context, configured, MOSS_PATCH_WHISPERING_WOODS, ValorConfiguredFeatures.MOSS_PATCH_WHISPERING_WOODS,
				surface(RarityFilter.onAverageOnceEvery(8), zone(0.2F, 1.0F, 0.0F)));

		// ---- Streams: most likely in the groves. The stream finds its own height, so no heightmap is needed.
		register(context, configured, STREAM_WHISPERING_WOODS, ValorConfiguredFeatures.STREAM_WHISPERING_WOODS, List.of(
				RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE,
				zone(0.5F, 0.3F, 1.0F), BiomeFilter.biome()));

		// ---- Small ground accents: darker soil in the Deepwood, a bit of coarse dirt everywhere.
		register(context, configured, DISK_PODZOL, ValorConfiguredFeatures.DISK_PODZOL,
				ground(5, zone(0.25F, 1.0F, 0.0F)));
		register(context, configured, DISK_COARSE_DIRT, ValorConfiguredFeatures.DISK_COARSE_DIRT,
				ground(4, zone(0.6F, 0.8F, 0.3F)));
		register(context, configured, DISK_ROOTED_DIRT, ValorConfiguredFeatures.DISK_ROOTED_DIRT,
				ground(6, zone(0.4F, 1.0F, 0.2F)));
	}

	private static WhisperingWoodsZoneFilter zone(float outerWoods, float deepwood, float hiddenGrove) {
		return WhisperingWoodsZoneFilter.of(outerWoods, deepwood, hiddenGrove);
	}

	/** Like vanilla's tree placement, with the zone check at the surface. */
	private static List<PlacementModifier> tree(int count, WhisperingWoodsZoneFilter zone) {
		return List.of(CountPlacement.of(count), InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(0),
				PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, zone, BiomeFilter.biome());
	}

	private static List<PlacementModifier> surface(PlacementModifier frequency, WhisperingWoodsZoneFilter zone) {
		return List.of(frequency, InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, zone, BiomeFilter.biome());
	}

	private static List<PlacementModifier> ground(int rarity, WhisperingWoodsZoneFilter zone) {
		return List.of(RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID,
				zone, BiomeFilter.biome());
	}

	private static void register(BootstrapContext<PlacedFeature> context, HolderGetter<ConfiguredFeature<?, ?>> configured,
			ResourceKey<PlacedFeature> key, ResourceKey<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> placement) {
		PlacementUtils.register(context, key, configured.getOrThrow(feature), placement);
	}

	private static ResourceKey<PlacedFeature> key(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Valor.id(name));
	}
}
