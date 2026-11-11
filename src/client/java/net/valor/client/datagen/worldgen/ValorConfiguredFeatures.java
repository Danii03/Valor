package net.valor.client.datagen.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.valor.Valor;
import net.valor.registry.ValorBlocks;
import net.valor.registry.ValorFeatures;
import net.valor.world.level.levelgen.feature.configurations.StreamConfiguration;
import net.valor.world.level.levelgen.feature.configurations.TemplateTreeConfiguration;

/**
 * Configured features (what to generate). Counterpart to vanilla's {@code TreeFeatures},
 * {@code VegetationFeatures}, … in {@code net.minecraft.data.worldgen.features}.
 * Where and how often they are generated, per forest zone, is decided in {@link ValorPlacedFeatures}.
 */
public final class ValorConfiguredFeatures {

	// Whispering Woods trees (templates in data/valor/structure/whispering_woods/).
	/** Medium tree, the backbone of the forest. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_1 = key("whispering_tree_1");
	/** Small tree. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_2 = key("whispering_tree_2");
	/** Huge "deepwood" tree with a 4×4 trunk. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_3 = key("whispering_tree_3");
	/** Rare leaning tree, only on hillsides or near water. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_4 = key("whispering_tree_4");

	/** Vanilla's small spruce, with Whispering Leaves. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_SPRUCE = key("whispering_spruce");

	public static final ResourceKey<ConfiguredFeature<?, ?>> STREAM_WHISPERING_WOODS = key("stream_whispering_woods");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MOSS_PATCH_WHISPERING_WOODS = key("moss_patch_whispering_woods");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_COARSE_DIRT = key("disk_coarse_dirt");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_PODZOL = key("disk_podzol");
	public static final ResourceKey<ConfiguredFeature<?, ?>> DISK_ROOTED_DIRT = key("disk_rooted_dirt");

	private ValorConfiguredFeatures() {}

	public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
		templateTree(context, WHISPERING_TREE_1, "whispering_tree_1", 2, false);
		templateTree(context, WHISPERING_TREE_2, "whispering_tree_2", 2, false);
		templateTree(context, WHISPERING_TREE_3, "whispering_tree_3", 3, false);
		templateTree(context, WHISPERING_TREE_4, "whispering_tree_4", 4, true);

		// Same shape as vanilla's TreeFeatures.SPRUCE; only the leaves differ.
		context.register(WHISPERING_SPRUCE, new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(Blocks.SPRUCE_LOG),
				new StraightTrunkPlacer(5, 2, 1),
				BlockStateProvider.simple(ValorBlocks.WHISPERING_LEAVES),
				new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
				new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build()));

		context.register(STREAM_WHISPERING_WOODS, new ConfiguredFeature<>(ValorFeatures.STREAM,
				new StreamConfiguration(UniformInt.of(12, 28), 3, 0.3F)));

		// A small, sparse cushion of moss with a few moss carpets: an accent, not a carpet. No azaleas.
		context.register(MOSS_PATCH_WHISPERING_WOODS, new ConfiguredFeature<>(Feature.VEGETATION_PATCH,
				new VegetationPatchConfiguration(BlockTags.MOSS_REPLACEABLE, BlockStateProvider.simple(Blocks.MOSS_BLOCK),
						PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MOSS_CARPET))),
						CaveSurface.FLOOR, ConstantInt.of(1), 0.0F, 5, 0.25F, UniformInt.of(1, 2), 0.3F)));

		groundDisk(context, DISK_COARSE_DIRT, Blocks.COARSE_DIRT, UniformInt.of(1, 2));
		groundDisk(context, DISK_PODZOL, Blocks.PODZOL, UniformInt.of(1, 2));
		groundDisk(context, DISK_ROOTED_DIRT, Blocks.ROOTED_DIRT, ConstantInt.of(1));
	}

	private static Holder<ConfiguredFeature<?, ?>> templateTree(BootstrapContext<ConfiguredFeature<?, ?>> context,
			ResourceKey<ConfiguredFeature<?, ?>> key, String template, int maxGroundStep, boolean requiresSlopeOrWater) {
		return context.register(key, new ConfiguredFeature<>(ValorFeatures.TEMPLATE_TREE,
				new TemplateTreeConfiguration(Valor.id("whispering_woods/" + template), maxGroundStep, requiresSlopeOrWater)));
	}

	/** Replaces the grass surface with a small patch of another ground block. */
	private static void groundDisk(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
			Block block, IntProvider radius) {
		context.register(key, new ConfiguredFeature<>(Feature.DISK, new DiskConfiguration(RuleBasedBlockStateProvider.simple(block),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), radius, 2)));
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, Valor.id(name));
	}
}
