package net.valor.client.datagen.worldgen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.valor.Valor;
import net.valor.registry.ValorFeatures;
import net.valor.world.level.levelgen.feature.configurations.StreamConfiguration;
import net.valor.world.level.levelgen.feature.configurations.TemplateTreeConfiguration;

/**
 * Configured features (what to generate). Counterpart to vanilla's {@code TreeFeatures},
 * {@code VegetationFeatures}, … in {@code net.minecraft.data.worldgen.features}.
 * Where they are generated is decided in {@link ValorPlacedFeatures}.
 */
public final class ValorConfiguredFeatures {

	// Whispering Woods trees (templates in data/valor/structure/whispering_woods/).
	/** Medium tree, the most common shape. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_1 = key("whispering_tree_1");
	/** Small tree. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_2 = key("whispering_tree_2");
	/** Huge "deepwood" tree with a 4×4 trunk. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_3 = key("whispering_tree_3");
	/** Rare leaning tree, only on hillsides or near water. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_4 = key("whispering_tree_4");
	public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_WHISPERING_WOODS = key("trees_whispering_woods");

	public static final ResourceKey<ConfiguredFeature<?, ?>> STREAM_WHISPERING_WOODS = key("stream_whispering_woods");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_PATCH_WHISPERING_WOODS = key("moss_patch_whispering_woods");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_COARSE_DIRT = key("disk_coarse_dirt");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_PODZOL = key("disk_podzol");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_ROOTED_DIRT = key("disk_rooted_dirt");

	/*
	 * Tree mix of the Whispering Woods, in percent of placed trees (before trees that do not fit are skipped).
	 * The last entry is the fallback and takes whatever share is left.
	 */
	private static final int WEIGHT_TREE_4 = 8;
	private static final int WEIGHT_TREE_3 = 18;
	private static final int WEIGHT_SPRUCE = 12;
	private static final int WEIGHT_TREE_2 = 22;
	private static final int WEIGHT_TREE_1 = 40;

	private ValorConfiguredFeatures() {}

	public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);

		Holder<ConfiguredFeature<?, ?>> tree1 = templateTree(context, WHISPERING_TREE_1, "whispering_tree_1", 2, false);
		Holder<ConfiguredFeature<?, ?>> tree2 = templateTree(context, WHISPERING_TREE_2, "whispering_tree_2", 2, false);
		Holder<ConfiguredFeature<?, ?>> tree3 = templateTree(context, WHISPERING_TREE_3, "whispering_tree_3", 3, false);
		Holder<ConfiguredFeature<?, ?>> tree4 = templateTree(context, WHISPERING_TREE_4, "whispering_tree_4", 4, true);

		context.register(TREES_WHISPERING_WOODS, new ConfiguredFeature<>(Feature.RANDOM_SELECTOR, weightedSelection(
				List.of(
						new Weighted(PlacementUtils.inlinePlaced(tree4), WEIGHT_TREE_4),
						new Weighted(PlacementUtils.inlinePlaced(tree3), WEIGHT_TREE_3),
						new Weighted(placed.getOrThrow(TreePlacements.SPRUCE_CHECKED), WEIGHT_SPRUCE),
						new Weighted(PlacementUtils.inlinePlaced(tree2), WEIGHT_TREE_2)),
				new Weighted(PlacementUtils.inlinePlaced(tree1), WEIGHT_TREE_1))));

		context.register(STREAM_WHISPERING_WOODS, new ConfiguredFeature<>(ValorFeatures.STREAM,
				new StreamConfiguration(UniformInt.of(12, 28), 3, 0.3F)));

		// Like vanilla's lush-cave moss patch, but for the forest floor and with sparser plants
		// (moss carpet, grass, azalea bushes), so it stays easy to walk through.
		context.register(MOSS_PATCH_WHISPERING_WOODS, new ConfiguredFeature<>(Feature.VEGETATION_PATCH,
				new VegetationPatchConfiguration(BlockTags.MOSS_REPLACEABLE, BlockStateProvider.simple(Blocks.MOSS_BLOCK),
						PlacementUtils.inlinePlaced(configured.getOrThrow(CaveFeatures.MOSS_VEGETATION)), CaveSurface.FLOOR,
						ConstantInt.of(1), 0.0F, 5, 0.35F, UniformInt.of(2, 4), 0.4F)));

		groundDisk(context, DISK_COARSE_DIRT, Blocks.COARSE_DIRT, UniformInt.of(2, 3));
		groundDisk(context, DISK_PODZOL, Blocks.PODZOL, UniformInt.of(2, 4));
		groundDisk(context, DISK_ROOTED_DIRT, Blocks.ROOTED_DIRT, UniformInt.of(1, 2));
	}

	private static Holder<ConfiguredFeature<?, ?>> templateTree(BootstrapContext<ConfiguredFeature<?, ?>> context,
			ResourceKey<ConfiguredFeature<?, ?>> key, String template, int maxGroundStep, boolean requiresSlopeOrWater) {
		return context.register(key, new ConfiguredFeature<>(ValorFeatures.TEMPLATE_TREE,
				new TemplateTreeConfiguration(Valor.id("whispering_woods/" + template), maxGroundStep, requiresSlopeOrWater)));
	}

	/** Replaces the grass surface with a patch of another ground block. */
	private static void groundDisk(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
			Block block, UniformInt radius) {
		context.register(key, new ConfiguredFeature<>(Feature.DISK, new DiskConfiguration(RuleBasedBlockStateProvider.simple(block),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), radius, 2)));
	}

	private record Weighted(Holder<PlacedFeature> feature, int weight) {}

	/**
	 * Vanilla's random selector tries its entries in order, each with its own chance. This turns
	 * absolute weights into those chances, so each entry ends up with {@code weight / total}.
	 */
	private static RandomFeatureConfiguration weightedSelection(List<Weighted> entries, Weighted fallback) {
		int remaining = fallback.weight() + entries.stream().mapToInt(Weighted::weight).sum();
		List<WeightedPlacedFeature> features = new ArrayList<>();
		for (Weighted entry : entries) {
			features.add(new WeightedPlacedFeature(entry.feature(), entry.weight() / (float) remaining));
			remaining -= entry.weight();
		}
		return new RandomFeatureConfiguration(features, fallback.feature());
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Valor.id(name));
	}
}
