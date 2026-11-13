package net.valor.client.datagen.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.valor.Valor;
import net.valor.tag.ValorBiomeTags;
import net.valor.world.level.levelgen.structure.ValorStructures;
import net.valor.world.level.levelgen.structure.structures.RuinArchaeology;
import net.valor.world.level.levelgen.structure.structures.RuinStructure;

/**
 * Structures and structure sets. Counterpart to vanilla's {@code Structures} and {@code StructureSets} in
 * {@code net.minecraft.data.worldgen}.
 * <p>
 * <b>Tuning the ruins</b> (all in the generated JSON, so a data pack can override it too):
 * <ul>
 *     <li>how often: {@code spacing}/{@code separation} (in chunks) and {@code frequency} of the structure set;</li>
 *     <li>which type: the {@code weight} of each structure in the set; a type whose spot is unsuitable hands
 *     over to the others;</li>
 *     <li>where in the forest: each structure's {@code zone_chance}, {@code max_ground_step}, {@code prefer_hollow};</li>
 *     <li>dig site: each structure's {@code archaeology}.</li>
 * </ul>
 */
public final class ValorStructureData {
	/** One candidate chunk per 12×12 chunks; neighboring candidates at least 6 chunks (about 100 blocks) apart. */
	private static final int RUIN_SPACING = 12;
	private static final int RUIN_SEPARATION = 5;
	private static final int RUIN_SALT = 0x7E11_A6E5;

	private ValorStructureData() {}

	public static void bootstrapStructures(BootstrapContext<Structure> context) {
		HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(ValorBiomeTags.HAS_WHISPERING_WOODS_RUINS);
		Structure.StructureSettings settings = new Structure.StructureSettings(biomes, Map.of(),
				GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);

		// A small, discreet dig site at the foot of the gate. Loves clearings, but also stands among the trees.
		context.register(ValorStructures.FALLEN_GATE, new RuinStructure(settings, Valor.id("whispering_woods/ruins/fallen_gate"),
				UniformInt.of(0, 1), 3, false, new RuinStructure.ZoneChance(0.45F, 0.35F, 1.0F), 0.12F,
				new RuinArchaeology(UniformInt.of(2, 3), UniformFloat.of(1.0F, 2.0F),
						weighted(Blocks.GRASS_BLOCK, 4, Blocks.COARSE_DIRT, 4, Blocks.COBBLESTONE, 2, Blocks.MOSSY_COBBLESTONE, 1, Blocks.GRAVEL, 1),
						UniformInt.of(1, 3), BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE,
						0.1F, 0.15F)));

		// Walls sunk one or two blocks into the ground, its room filled with earth: the richest dig site.
		context.register(ValorStructures.SUNKEN_RUIN, new RuinStructure(settings, Valor.id("whispering_woods/ruins/sunken_ruin"),
				UniformInt.of(1, 2), 2, true, new RuinStructure.ZoneChance(0.5F, 0.6F, 0.8F), 0.15F,
				new RuinArchaeology(UniformInt.of(3, 5), UniformFloat.of(1.2F, 2.6F),
						weighted(Blocks.GRAVEL, 4, Blocks.COARSE_DIRT, 4, Blocks.GRASS_BLOCK, 2, Blocks.COBBLESTONE, 1, Blocks.MOSSY_COBBLESTONE, 1),
						UniformInt.of(3, 6), BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE,
						0.15F, 0.3F)));

		// The tower's floor is level with the ground; rubble of fallen stones around it, partly buried.
		context.register(ValorStructures.TOWER_RUIN, new RuinStructure(settings, Valor.id("whispering_woods/ruins/tower_ruin"),
				ConstantInt.of(1), 3, false, new RuinStructure.ZoneChance(0.7F, 0.5F, 0.35F), 0.1F,
				new RuinArchaeology(UniformInt.of(2, 4), UniformFloat.of(1.0F, 2.2F),
						weighted(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 4, Blocks.COARSE_DIRT, 2, Blocks.GRAVEL, 2, Blocks.GRASS_BLOCK, 2),
						UniformInt.of(2, 4), BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE,
						0.2F, 0.35F)));
	}

	public static void bootstrapStructureSets(BootstrapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
		// Fallen gates most often, towers rarely. A type whose spot is unsuitable hands over to the others, so the
		// weights are lower for the types that fit most spots. Measured offline with vanilla terrain on 5 seeds:
		// about 10 ruins per 1000x1000 blocks of forest, gates : sunken ruins : towers about 4 : 3 : 1.
		// Kept away from vanilla's trail ruins, so ruins never cluster.
		context.register(ValorStructures.WHISPERING_WOODS_RUINS, new StructureSet(
				List.of(
						StructureSet.entry(structures.getOrThrow(ValorStructures.FALLEN_GATE), 7),
						StructureSet.entry(structures.getOrThrow(ValorStructures.SUNKEN_RUIN), 4),
						StructureSet.entry(structures.getOrThrow(ValorStructures.TOWER_RUIN), 1)),
				new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, RUIN_SALT,
						Optional.of(new StructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.TRAIL_RUINS), 4)),
						RUIN_SPACING, RUIN_SEPARATION, RandomSpreadType.LINEAR)));
	}

	/** A weighted mix of blocks, given as block, weight, block, weight, … */
	private static WeightedStateProvider weighted(Object... blocksAndWeights) {
		SimpleWeightedRandomList.Builder<BlockState> builder = SimpleWeightedRandomList.builder();
		for (int i = 0; i < blocksAndWeights.length; i += 2) {
			builder.add(((Block) blocksAndWeights[i]).defaultBlockState(), (Integer) blocksAndWeights[i + 1]);
		}
		return new WeightedStateProvider(builder);
	}
}
