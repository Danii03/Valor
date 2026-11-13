package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.valor.Valor;
import net.valor.world.level.levelgen.feature.configurations.GroundTemplateConfiguration;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateBlocks;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateTreeProcessor;

/**
 * Lays a hand-built template ({@code .nbt}) onto the ground: old stumps, fallen trunks, other deadwood.
 * Unlike {@link TemplateTreeFeature} nothing grows upwards; the template rests on the terrain and the
 * terrain settles around it:
 * <ul>
 *     <li><b>Random orientation</b>: all 4 rotations × mirroring, turned around the footprint's center.</li>
 *     <li><b>Resting height</b>: the ground under every column of the footprint is measured. Spots steeper
 *     than {@link GroundTemplateConfiguration#maxGroundStep()} are skipped. The template rests at the
 *     middle height, so on a slope it is a little sunk into the uphill side and the gap on the downhill
 *     side is filled with soil: it never floats and is never cut by a wall of terrain. Sometimes it is
 *     sunk one more block, as if it had settled over the years.</li>
 *     <li><b>Terrain blending</b>: soil filled under the template continues as a gentle mound into the
 *     surroundings; the ground around it may turn to podzol, coarse dirt or moss; moss settles on top.</li>
 *     <li><b>No overlaps</b>: skipped if any part would hit water, leaves, logs, rocks or an overhang,
 *     or if another tree or piece of deadwood is within {@link GroundTemplateConfiguration#clearance()}.
 *     Air in the template never carves terrain.</li>
 * </ul>
 * Meant to run before the undergrowth, so grass and ferns grow around (and inside) the deadwood.
 */
public class GroundTemplateFeature extends Feature<GroundTemplateConfiguration> {
	private static final int MAX_GROUND_SEARCH = 40;
	private static final int MOUND_RADIUS = 2;
	private static final int MOUND_MAX_RAISE = 2;

	private static final Map<StructureTemplate, Shape> SHAPES = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Set<ResourceLocation> REPORTED_MISSING = ConcurrentHashMap.newKeySet();

	public GroundTemplateFeature(Codec<GroundTemplateConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<GroundTemplateConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		GroundTemplateConfiguration config = context.config();

		Optional<StructureTemplate> optionalTemplate = level.getLevel().getServer().getStructureManager().get(config.template());
		if (optionalTemplate.isEmpty()) {
			if (REPORTED_MISSING.add(config.template())) {
				Valor.LOGGER.error("Missing template {}", config.template());
			}
			return false;
		}
		StructureTemplate template = optionalTemplate.get();
		Shape shape = SHAPES.computeIfAbsent(template, Shape::of);
		if (shape.blocks().isEmpty()) {
			return false;
		}

		int originGround = TerrainFitting.findGround(level, origin.getX(), origin.getZ(), origin.getY() + 1, MAX_GROUND_SEARCH);
		if (originGround == TerrainFitting.NO_GROUND) {
			return false;
		}
		if (random.nextFloat() >= config.flatGroundChance()
				&& !TerrainFitting.isNearSlopeOrWater(level, new BlockPos(origin.getX(), originGround, origin.getZ()))) {
			return false;
		}

		Rotation rotation = Rotation.getRandom(random);
		Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
		BlockPos pivot = shape.pivot();

		// Ground under every column of the footprint.
		int step = config.maxGroundStep();
		int highest = Integer.MIN_VALUE;
		int lowest = Integer.MAX_VALUE;
		Map<Long, Integer> ground = new LinkedHashMap<>();
		for (BlockPos column : shape.columns()) {
			BlockPos world = StructureTemplate.transform(column, mirror, rotation, pivot).subtract(pivot);
			int x = origin.getX() + world.getX();
			int z = origin.getZ() + world.getZ();
			int top = originGround + step + 1;
			int height = TerrainFitting.findGround(level, x, z, top, 2 * step + 2);
			if (height == TerrainFitting.NO_GROUND || height == top) {
				return false; // water, a rock, a log, or a wall of terrain
			}
			ground.put(BlockPos.asLong(x, 0, z), height);
			highest = Math.max(highest, height);
			lowest = Math.min(lowest, height);
		}
		if (highest - lowest > step) {
			return false;
		}

		// Rest at the middle height; sometimes settled one block deeper.
		int restingGround = (highest + lowest + 1) / 2;
		int bottomY = restingGround + 1 - (random.nextFloat() < config.sinkChance() ? 1 : 0);
		BlockPos offset = new BlockPos(origin.getX() - pivot.getX(), bottomY - shape.minY(), origin.getZ() - pivot.getZ());

		BoundingBox writable = TerrainFitting.writableArea(level, origin);
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(rotation)
				.setMirror(mirror)
				.setRotationPivot(pivot)
				.setBoundingBox(writable)
				.setRandom(random)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
				.addProcessor(TemplateTreeProcessor.INSTANCE);

		BoundingBox box = template.getBoundingBox(settings, offset);
		int reach = Math.max(MOUND_RADIUS, config.groundCover().map(cover -> cover.radius()).orElse(0)) + 1;
		if (!writable.isInside(box.minX() - reach, box.minY(), box.minZ() - reach)
				|| !writable.isInside(box.maxX() + reach, box.maxY(), box.maxZ() + reach)) {
			return false;
		}
		if (!hasRoom(level, shape, offset, mirror, rotation, pivot, ground)) {
			return false;
		}
		if (!hasClearance(level, box, config.clearance())) {
			return false;
		}

		// Soil under the low side, continued as a gentle mound.
		Map<Long, Integer> footprint = new HashMap<>();
		for (Map.Entry<Long, Integer> entry : ground.entrySet()) {
			BlockPos column = BlockPos.of(entry.getKey());
			if (entry.getValue() < bottomY - 1) {
				TerrainFitting.fillSoil(level, column.getX(), column.getZ(), entry.getValue(), bottomY - 1);
			}
			footprint.put(entry.getKey(), Math.max(entry.getValue(), bottomY - 1));
		}
		TerrainFitting.raiseSurroundings(level, random, footprint, MOUND_RADIUS, MOUND_MAX_RAISE, writable);

		template.placeInWorld(level, offset, offset, settings, random, Block.UPDATE_CLIENTS);

		if (config.mossChance() > 0.0F) {
			growMoss(level, random, shape, offset, mirror, rotation, pivot, config.mossChance());
		}
		config.groundCover().ifPresent(cover -> TerrainFitting.coverGround(level, random, footprint.keySet(), box.maxY() + 1, cover, writable));
		TerrainFitting.removeUnsupportedPlants(level, TerrainFitting.clamp(new BoundingBox(
				box.minX() - reach, lowest - 1, box.minZ() - reach,
				box.maxX() + reach, box.maxY() + 2, box.maxZ() + reach), writable));
		return true;
	}

	/**
	 * Every block of the template must land in air or plants above the ground of its column (or in soil
	 * below it, where the template is sunk in), and never in water, leaves, logs, rocks or an overhang.
	 */
	private static boolean hasRoom(WorldGenLevel level, Shape shape, BlockPos offset, Mirror mirror, Rotation rotation,
			BlockPos pivot, Map<Long, Integer> ground) {
		for (StructureTemplate.StructureBlockInfo info : shape.blocks()) {
			BlockPos pos = StructureTemplate.transform(info.pos(), mirror, rotation, pivot).offset(offset);
			BlockState state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
					|| state.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
				return false;
			}
			Integer columnGround = ground.get(BlockPos.asLong(pos.getX(), 0, pos.getZ()));
			boolean belowGround = columnGround != null && pos.getY() <= columnGround;
			if (belowGround ? !TerrainFitting.isSoil(state) && !TerrainFitting.isAirOrPlant(state) : !TerrainFitting.isAirOrPlant(state)) {
				return false;
			}
		}
		return true;
	}

	/** No other tree or deadwood right next to this one. */
	private static boolean hasClearance(WorldGenLevel level, BoundingBox box, int clearance) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = box.minX() - clearance; x <= box.maxX() + clearance; x++) {
			for (int z = box.minZ() - clearance; z <= box.maxZ() + clearance; z++) {
				for (int y = box.minY(); y <= box.maxY() + 1; y++) {
					if (level.getBlockState(pos.set(x, y, z)).is(BlockTags.LOGS)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Moss carpets on some of the exposed, flat tops of the wood. */
	private static void growMoss(WorldGenLevel level, RandomSource random, Shape shape, BlockPos offset, Mirror mirror,
			Rotation rotation, BlockPos pivot, float chance) {
		for (StructureTemplate.StructureBlockInfo info : shape.blocks()) {
			if (!info.state().is(BlockTags.LOGS) || random.nextFloat() >= chance) {
				continue;
			}
			BlockPos pos = StructureTemplate.transform(info.pos(), mirror, rotation, pivot).offset(offset);
			BlockPos above = pos.above();
			if (level.getBlockState(pos).is(BlockTags.LOGS) && level.getBlockState(above).isAir()) {
				level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	/**
	 * The parts of a template that matter for placement, in template coordinates.
	 *
	 * @param blocks  every non-air block
	 * @param columns every x/z column holding at least one block, at y 0
	 * @param minY    lowest layer with blocks; it rests on the ground
	 * @param pivot   center of the footprint at y 0; the template turns around it
	 */
	private record Shape(List<StructureTemplate.StructureBlockInfo> blocks, List<BlockPos> columns, int minY, BlockPos pivot) {

		static Shape of(StructureTemplate template) {
			List<StructureTemplate.StructureBlockInfo> blocks = TemplateBlocks.of(template);
			if (blocks.isEmpty()) {
				return new Shape(List.of(), List.of(), 0, BlockPos.ZERO);
			}
			int minY = blocks.stream().mapToInt(info -> info.pos().getY()).min().orElseThrow();
			List<BlockPos> columns = blocks.stream().map(info -> new BlockPos(info.pos().getX(), 0, info.pos().getZ())).distinct().toList();
			int minX = columns.stream().mapToInt(BlockPos::getX).min().orElseThrow();
			int maxX = columns.stream().mapToInt(BlockPos::getX).max().orElseThrow();
			int minZ = columns.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
			int maxZ = columns.stream().mapToInt(BlockPos::getZ).max().orElseThrow();
			return new Shape(blocks, columns, minY, new BlockPos((minX + maxX) / 2, 0, (minZ + maxZ) / 2));
		}
	}
}
