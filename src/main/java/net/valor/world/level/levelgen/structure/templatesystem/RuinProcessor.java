package net.valor.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.valor.registry.ValorStructureProcessorTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Lets a ruin settle into the world instead of overwriting it: its stones may stand in soil, rock and
 * plants (sunk walls, overgrown bases), but never cut a tree trunk, never displace water and never replace
 * protected blocks. Use together with {@code BlockIgnoreProcessor.STRUCTURE_AND_AIR}, so the template's air
 * never carves terrain and sunken rooms stay filled with earth.
 */
public final class RuinProcessor extends StructureProcessor {
	public static final RuinProcessor INSTANCE = new RuinProcessor();
	public static final MapCodec<RuinProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

	private RuinProcessor() {}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot,
			StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
			StructurePlaceSettings settings) {
		BlockState existing = level.getBlockState(current.pos());
		boolean blocked = existing.is(BlockTags.LOGS)
				|| existing.is(BlockTags.FEATURES_CANNOT_REPLACE)
				|| !existing.getFluidState().isEmpty();
		return blocked ? null : current;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return ValorStructureProcessorTypes.RUIN;
	}
}
