package net.valor.client.datagen.worldgen;

import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.valor.Valor;

/**
 * Placed features (where and how often). Counterpart to vanilla's {@code VegetationPlacements},
 * {@code TreePlacements}, … in {@code net.minecraft.data.worldgen.placement}.
 */
public final class ValorPlacedFeatures {

	public static final ResourceKey<PlacedFeature> TREES_WHISPERING_WOODS = key("trees_whispering_woods");
	public static final ResourceKey<PlacedFeature> STREAM_WHISPERING_WOODS = key("stream_whispering_woods");
	public static final ResourceKey<PlacedFeature> MOSS_PATCH_WHISPERING_WOODS = key("moss_patch_whispering_woods");
	public static final ResourceKey<PlacedFeature> PATCH_TALL_GRASS_WHISPERING_WOODS = key("patch_tall_grass_whispering_woods");
	public static final ResourceKey<PlacedFeature> DISK_COARSE_DIRT = key("disk_coarse_dirt");
	public static final ResourceKey<PlacedFeature> DISK_PODZOL = key("disk_podzol");
	public static final ResourceKey<PlacedFeature> DISK_ROOTED_DIRT = key("disk_rooted_dirt");

	/** Tree attempts per chunk. Most chunks are dense; some are thin, which reads as small clearings. */
	private static final int TREE_ATTEMPTS_DENSE = 11;
	private static final int TREE_ATTEMPTS_CLEARING = 2;
	private static final int CLEARING_WEIGHT = 1;
	private static final int DENSE_WEIGHT = 6;

	private ValorPlacedFeatures() {}

	public static void bootstrap(BootstrapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		IntProvider treeCount = new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
				.add(ConstantInt.of(TREE_ATTEMPTS_DENSE), DENSE_WEIGHT)
				.add(ConstantInt.of(TREE_ATTEMPTS_CLEARING), CLEARING_WEIGHT)
				.build());
		PlacementUtils.register(context, TREES_WHISPERING_WOODS, configured.getOrThrow(ValorConfiguredFeatures.TREES_WHISPERING_WOODS),
				VegetationPlacements.treePlacement(CountPlacement.of(treeCount)));

		// The stream finds its own height, so no heightmap modifier is needed.
		PlacementUtils.register(context, STREAM_WHISPERING_WOODS, configured.getOrThrow(ValorConfiguredFeatures.STREAM_WHISPERING_WOODS),
				RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), BiomeFilter.biome());

		PlacementUtils.register(context, MOSS_PATCH_WHISPERING_WOODS, configured.getOrThrow(ValorConfiguredFeatures.MOSS_PATCH_WHISPERING_WOODS),
				CountPlacement.of(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

		PlacementUtils.register(context, PATCH_TALL_GRASS_WHISPERING_WOODS, configured.getOrThrow(VegetationFeatures.PATCH_TALL_GRASS),
				RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());

		groundDisk(context, configured, DISK_COARSE_DIRT, ValorConfiguredFeatures.DISK_COARSE_DIRT, 2);
		groundDisk(context, configured, DISK_PODZOL, ValorConfiguredFeatures.DISK_PODZOL, 3);
		groundDisk(context, configured, DISK_ROOTED_DIRT, ValorConfiguredFeatures.DISK_ROOTED_DIRT, 2);
	}

	private static void groundDisk(BootstrapContext<PlacedFeature> context, HolderGetter<ConfiguredFeature<?, ?>> configured,
			ResourceKey<PlacedFeature> key, ResourceKey<ConfiguredFeature<?, ?>> feature, int rarity) {
		PlacementUtils.register(context, key, configured.getOrThrow(feature), List.of(
				RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome()));
	}

	private static ResourceKey<PlacedFeature> key(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Valor.id(name));
	}
}
