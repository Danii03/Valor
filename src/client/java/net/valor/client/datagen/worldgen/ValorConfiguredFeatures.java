package net.valor.client.datagen.worldgen;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.valor.Valor;
import net.valor.registry.ValorBlocks;
import net.valor.registry.ValorFeatures;
import net.valor.world.level.levelgen.feature.configurations.GroundCover;
import net.valor.world.level.levelgen.feature.configurations.GroundTemplateConfiguration;
import net.valor.world.level.levelgen.feature.configurations.RockFormationConfiguration;
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
	/** Monumental ancient giant, a landmark of the Deepwood. Stands on its own and is rooted into the terrain. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHISPERING_TREE_5 = key("whispering_tree_5");

	// Deadwood and rocks: the signs of a very old forest.
	/** A thick, weathered stump, sometimes settled into the ground. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_STUMP = key("huge_stump");
	/** A hollow fallen trunk, preferably on hillsides and along streams. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> FALLEN_STUMP = key("fallen_stump");
	/** A small group of mossy, half-sunken boulders. */
	public static final ResourceKey<ConfiguredFeature<?, ?>> ROCK_FORMATION_WHISPERING_WOODS = key("rock_formation_whispering_woods");

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
		// The giant needs room of its own (no foreign trunk in its crown), and the ground rises around its roots.
		context.register(WHISPERING_TREE_5, new ConfiguredFeature<>(ValorFeatures.TEMPLATE_TREE, new TemplateTreeConfiguration(
				template("whispering_tree_5"), 3, false, 2, true,
				Optional.of(new GroundCover(weighted(Blocks.PODZOL, 4, Blocks.COARSE_DIRT, 2, Blocks.ROOTED_DIRT, 2, Blocks.MOSS_BLOCK, 1), 3, 0.6F)))));

		// Deadwood: middle height on slopes, gaps filled with soil, decayed soil and a little moss around it.
		context.register(HUGE_STUMP, new ConfiguredFeature<>(ValorFeatures.GROUND_TEMPLATE, new GroundTemplateConfiguration(
				template("huge_stump"), 2, 0.5F, 2, 1.0F, 0.2F,
				Optional.of(new GroundCover(weighted(Blocks.PODZOL, 3, Blocks.COARSE_DIRT, 2, Blocks.ROOTED_DIRT, 1, Blocks.MOSS_BLOCK, 2), 2, 0.5F)))));
		context.register(FALLEN_STUMP, new ConfiguredFeature<>(ValorFeatures.GROUND_TEMPLATE, new GroundTemplateConfiguration(
				template("fallen_stump"), 2, 0.3F, 1, 0.35F, 0.15F,
				Optional.of(new GroundCover(weighted(Blocks.PODZOL, 2, Blocks.COARSE_DIRT, 1, Blocks.MOSS_BLOCK, 2), 1, 0.35F)))));

		// Natural boulders: stone and cobblestone bodies, mossy tops. No bricks; these are not ruins.
		context.register(ROCK_FORMATION_WHISPERING_WOODS, new ConfiguredFeature<>(ValorFeatures.ROCK_FORMATION, new RockFormationConfiguration(
				weighted(Blocks.STONE, 5, Blocks.COBBLESTONE, 3, Blocks.MOSSY_COBBLESTONE, 3, Blocks.ANDESITE, 1),
				weighted(Blocks.MOSSY_COBBLESTONE, 5, Blocks.STONE, 2, Blocks.MOSS_BLOCK, 1),
				UniformInt.of(1, 4), UniformFloat.of(1.2F, 2.3F), 4, 0.4F, 0.2F)));

		// Same shape as vanilla's TreeFeatures.SPRUCE; only the leaves differ.
		context.register(WHISPERING_SPRUCE, new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(Blocks.SPRUCE_LOG),
				new StraightTrunkPlacer(5, 2, 1),
				BlockStateProvider.simple(ValorBlocks.WHISPERING_LEAVES),
				new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
				new TwoLayersFeatureSize(2, 0, 2)).ignoreVines().build()));

		// Winding streams of 1 to 6 blocks width, with banks sloping down over up to 5 blocks.
		context.register(STREAM_WHISPERING_WOODS, new ConfiguredFeature<>(ValorFeatures.STREAM,
				new StreamConfiguration(UniformInt.of(24, 40), 3, 5, 0.35F)));

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
				new TemplateTreeConfiguration(template(template), maxGroundStep, requiresSlopeOrWater)));
	}

	private static ResourceLocation template(String name) {
		return Valor.id("whispering_woods/" + name);
	}

	/** A weighted mix of blocks, given as block, weight, block, weight, … */
	private static WeightedStateProvider weighted(Object... blocksAndWeights) {
		SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
		for (int i = 0; i < blocksAndWeights.length; i += 2) {
			builder.add(((Block) blocksAndWeights[i]).defaultBlockState(), (Integer) blocksAndWeights[i + 1]);
		}
		return new WeightedStateProvider(builder);
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
