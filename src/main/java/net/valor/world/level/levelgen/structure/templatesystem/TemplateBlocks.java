package net.valor.world.level.levelgen.structure.templatesystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Every non-air block of a structure template, in template coordinates (first palette).
 * <p>
 * {@link StructureTemplate} only exposes its blocks filtered by block type, so they are read back
 * from the template's own NBT form. Cached per template.
 */
public final class TemplateBlocks {
	private static final Map<StructureTemplate, List<StructureTemplate.StructureBlockInfo>> CACHE =
			Collections.synchronizedMap(new WeakHashMap<>());

	private TemplateBlocks() {}

	public static List<StructureTemplate.StructureBlockInfo> of(StructureTemplate template) {
		return CACHE.computeIfAbsent(template, TemplateBlocks::read);
	}

	private static List<StructureTemplate.StructureBlockInfo> read(StructureTemplate template) {
		CompoundTag tag = template.save(new CompoundTag());
		ListTag paletteTag = tag.contains(StructureTemplate.PALETTE_LIST_TAG, Tag.TAG_LIST)
				? tag.getList(StructureTemplate.PALETTE_LIST_TAG, Tag.TAG_LIST).getList(0)
				: tag.getList(StructureTemplate.PALETTE_TAG, Tag.TAG_COMPOUND);
		HolderGetter<Block> blocks = BuiltInRegistries.BLOCK.asLookup();
		List<BlockState> palette = new ArrayList<>(paletteTag.size());
		for (int i = 0; i < paletteTag.size(); i++) {
			palette.add(NbtUtils.readBlockState(blocks, paletteTag.getCompound(i)));
		}

		List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>();
		ListTag blockList = tag.getList(StructureTemplate.BLOCKS_TAG, Tag.TAG_COMPOUND);
		for (int i = 0; i < blockList.size(); i++) {
			CompoundTag block = blockList.getCompound(i);
			int index = block.getInt(StructureTemplate.BLOCK_TAG_STATE);
			if (index < 0 || index >= palette.size() || palette.get(index).isAir()) {
				continue;
			}
			ListTag pos = block.getList(StructureTemplate.BLOCK_TAG_POS, Tag.TAG_INT);
			result.add(new StructureTemplate.StructureBlockInfo(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
					palette.get(index), null));
		}
		return List.copyOf(result);
	}
}
