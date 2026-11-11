package net.valor.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.valor.registry.ValorBlocks;
import net.valor.registry.ValorStructureProcessorTypes;
import net.valor.world.level.levelgen.feature.TemplateTreeFeature;
import org.jetbrains.annotations.Nullable;

/**
 * Lets a tree template blend into the world instead of overwriting it, like vanilla tree features:
 * <ul>
 *     <li>logs may grow into soil and plants, but never into other logs, fluids or protected blocks;</li>
 *     <li>leaves (and anything else) only fill air and plants, so canopies of neighboring trees
 *     merge instead of cutting into each other, and terrain is never hollowed out.</li>
 * </ul>
 * Also turns the template's spruce leaves into {@link ValorBlocks#WHISPERING_LEAVES} while placing, so the
 * original {@code .nbt} files stay untouched. The templates were built by hand, so their leaves are
 * persistent; placed leaves are made non-persistent and keep their stored distance (at most 3 in all
 * templates), so they decay like a natural tree's when the trunk is cut.
 * <p>
 * Use together with {@code BlockIgnoreProcessor.STRUCTURE_AND_AIR}, so air in the template is skipped.
 */
public final class TemplateTreeProcessor extends StructureProcessor {
	public static final TemplateTreeProcessor INSTANCE = new TemplateTreeProcessor();
	public static final MapCodec<TemplateTreeProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

	private TemplateTreeProcessor() {}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pivot,
			StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo current,
			StructurePlaceSettings settings) {
		if (current.state().is(Blocks.SPRUCE_LEAVES)) {
			current = new StructureTemplate.StructureBlockInfo(current.pos(), whisperingLeaves(current.state()), current.nbt());
		}

		BlockState existing = level.getBlockState(current.pos());
		if (current.state().is(BlockTags.LOGS)) {
			boolean blocked = existing.is(BlockTags.LOGS)
					|| existing.is(BlockTags.FEATURES_CANNOT_REPLACE)
					|| !existing.getFluidState().isEmpty();
			return blocked ? null : current;
		}
		return existing.isAir() || TemplateTreeFeature.isPlant(existing) ? current : null;
	}

	private static BlockState whisperingLeaves(BlockState spruceLeaves) {
		return ValorBlocks.WHISPERING_LEAVES.defaultBlockState()
				.setValue(LeavesBlock.DISTANCE, spruceLeaves.getValue(LeavesBlock.DISTANCE))
				.setValue(LeavesBlock.WATERLOGGED, spruceLeaves.getValue(LeavesBlock.WATERLOGGED))
				.setValue(LeavesBlock.PERSISTENT, false);
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return ValorStructureProcessorTypes.TEMPLATE_TREE;
	}
}
