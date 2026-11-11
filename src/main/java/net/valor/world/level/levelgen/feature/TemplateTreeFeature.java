package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
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
import net.valor.world.level.levelgen.feature.configurations.TemplateTreeConfiguration;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateTreeProcessor;

/**
 * Grows a hand-built tree from a structure template ({@code .nbt}), with the care vanilla tree
 * features take:
 * <ul>
 *     <li><b>Random orientation</b>: all 4 rotations × mirroring, turned around the trunk.</li>
 *     <li><b>Anchoring</b>: every trunk column at the template's lowest layer must stand on soil
 *     within {@link TemplateTreeConfiguration#maxGroundStep()} blocks. The trunk sits on the highest
 *     of them; on the low side the trunk is extended down to the ground like roots, so trees never
 *     float on slopes. Soil under the trunk becomes rooted dirt.</li>
 *     <li><b>No overlaps</b>: a tree is skipped if its trunk would hit another trunk, water or a
 *     cliff. Leaves only fill air and plants, see {@link TemplateTreeProcessor}; air in the template
 *     never carves terrain or neighboring trees.</li>
 *     <li><b>Chunk safe</b>: trees are only placed if they fit in the area a feature may write to.</li>
 * </ul>
 * Expected to run after the ground vegetation, so undergrowth also grows under canopies.
 */
public class TemplateTreeFeature extends Feature<TemplateTreeConfiguration> {
	/** How far down the trunk position is searched for soil (through canopies of earlier trees). */
	private static final int MAX_GROUND_SEARCH = 40;
	/** At most this fraction of the logs may end up inside terrain (hillsides); more means a cliff or overhang. */
	private static final float MAX_EMBEDDED_LOG_FRACTION = 0.2F;
	private static final int WATER_SEARCH_RADIUS = 6;
	private static final int SLOPE_SAMPLE_DISTANCE = 4;
	private static final int SLOPE_MIN_HEIGHT_DIFFERENCE = 2;
	/** Features may write into the chunk they decorate and its direct neighbors. */
	private static final int WRITE_MARGIN = 16;

	private static final Map<StructureTemplate, Shape> SHAPES = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Set<ResourceLocation> REPORTED_MISSING = ConcurrentHashMap.newKeySet();

	public TemplateTreeFeature(Codec<TemplateTreeConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<TemplateTreeConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		TemplateTreeConfiguration config = context.config();

		Optional<StructureTemplate> optionalTemplate = level.getLevel().getServer().getStructureManager().get(config.template());
		if (optionalTemplate.isEmpty()) {
			if (REPORTED_MISSING.add(config.template())) {
				Valor.LOGGER.error("Missing tree template {}", config.template());
			}
			return false;
		}
		StructureTemplate template = optionalTemplate.get();
		Shape shape = SHAPES.computeIfAbsent(template, Shape::of);
		if (shape.contact().isEmpty()) {
			return false;
		}

		int pivotGround = findGround(level, origin.getX(), origin.getZ(), origin.getY() + 1, MAX_GROUND_SEARCH);
		if (pivotGround == NO_GROUND) {
			return false;
		}
		if (config.requiresSlopeOrWater() && !isNearSlopeOrWater(level, new BlockPos(origin.getX(), pivotGround, origin.getZ()))) {
			return false;
		}

		Rotation rotation = Rotation.getRandom(random);
		// Four rotations of one mirror image cover all eight orientations.
		Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
		BlockPos pivot = shape.pivot();
		BlockPos rotatedPivot = StructureTemplate.transform(pivot, mirror, rotation, pivot);

		// Ground under every trunk column at the template's lowest layer.
		int step = config.maxGroundStep();
		int highestGround = pivotGround;
		int lowestGround = pivotGround;
		List<BlockPos> contactWorld = new ArrayList<>(shape.contact().size());
		int[] contactGround = new int[shape.contact().size()];
		for (int i = 0; i < shape.contact().size(); i++) {
			BlockPos local = StructureTemplate.transform(shape.contact().get(i).pos(), mirror, rotation, pivot).subtract(rotatedPivot);
			int x = origin.getX() + local.getX();
			int z = origin.getZ() + local.getZ();
			int ground = findGround(level, x, z, pivotGround + step + 1, 2 * step + 2);
			if (ground == NO_GROUND || ground == pivotGround + step + 1) {
				return false; // no soil, water, or a wall of terrain
			}
			contactGround[i] = ground;
			contactWorld.add(new BlockPos(x, 0, z));
			highestGround = Math.max(highestGround, ground);
			lowestGround = Math.min(lowestGround, ground);
		}
		if (highestGround - lowestGround > step) {
			return false;
		}

		int baseY = highestGround + 1;
		BlockPos offset = new BlockPos(origin.getX() - rotatedPivot.getX(), baseY - rotatedPivot.getY(), origin.getZ() - rotatedPivot.getZ());

		ChunkPos chunk = new ChunkPos(origin);
		BoundingBox writable = new BoundingBox(
				chunk.getMinBlockX() - WRITE_MARGIN, level.getMinBuildHeight(), chunk.getMinBlockZ() - WRITE_MARGIN,
				chunk.getMaxBlockX() + WRITE_MARGIN, level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ() + WRITE_MARGIN);
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(rotation)
				.setMirror(mirror)
				.setRotationPivot(pivot)
				.setBoundingBox(writable)
				.setRandom(random)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
				.addProcessor(TemplateTreeProcessor.INSTANCE);

		BoundingBox treeBox = template.getBoundingBox(settings, offset);
		if (!writable.isInside(treeBox.minX(), treeBox.minY(), treeBox.minZ())
				|| !writable.isInside(treeBox.maxX(), treeBox.maxY(), treeBox.maxZ())) {
			return false;
		}
		if (!hasRoomForTrunk(level, shape, offset, mirror, rotation, pivot)) {
			return false;
		}

		// Roots on the low side of a slope, so the trunk never floats.
		for (int i = 0; i < contactWorld.size(); i++) {
			BlockState rootState = shape.contact().get(i).state().mirror(mirror).rotate(rotation);
			BlockPos column = contactWorld.get(i);
			for (int y = contactGround[i] + 1; y < baseY; y++) {
				BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
				if (isAirOrPlant(level.getBlockState(pos))) {
					level.setBlock(pos, rootState, Block.UPDATE_CLIENTS);
				}
			}
			BlockPos soil = new BlockPos(column.getX(), contactGround[i], column.getZ());
			BlockState soilState = level.getBlockState(soil);
			if (soilState.is(Blocks.GRASS_BLOCK) || soilState.is(Blocks.DIRT) || soilState.is(Blocks.PODZOL)) {
				level.setBlock(soil, Blocks.ROOTED_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}

		return template.placeInWorld(level, offset, offset, settings, random, Block.UPDATE_CLIENTS);
	}

	/** Trunks must not cross other trunks or fluids, and may only be partly sunk into a hillside. */
	private static boolean hasRoomForTrunk(WorldGenLevel level, Shape shape, BlockPos offset, Mirror mirror,
			Rotation rotation, BlockPos pivot) {
		int embedded = 0;
		int maxEmbedded = (int) (shape.logs().size() * MAX_EMBEDDED_LOG_FRACTION);
		for (StructureTemplate.StructureBlockInfo log : shape.logs()) {
			BlockPos pos = StructureTemplate.transform(log.pos(), mirror, rotation, pivot).offset(offset);
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.LOGS) || state.is(BlockTags.FEATURES_CANNOT_REPLACE) || !state.getFluidState().isEmpty()) {
				return false;
			}
			if (!state.isAir() && !state.is(BlockTags.LEAVES) && !isPlant(state) && ++embedded > maxEmbedded) {
				return false;
			}
		}
		return true;
	}

	private static boolean isNearSlopeOrWater(WorldGenLevel level, BlockPos ground) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -WATER_SEARCH_RADIUS; dx <= WATER_SEARCH_RADIUS; dx++) {
			for (int dz = -WATER_SEARCH_RADIUS; dz <= WATER_SEARCH_RADIUS; dz++) {
				for (int dy = -2; dy <= 1; dy++) {
					if (level.getFluidState(pos.setWithOffset(ground, dx, dy, dz)).is(FluidTags.WATER)) {
						return true;
					}
				}
			}
		}
		int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		for (int[] direction : directions) {
			int x = ground.getX() + direction[0] * SLOPE_SAMPLE_DISTANCE;
			int z = ground.getZ() + direction[1] * SLOPE_SAMPLE_DISTANCE;
			int height = findGround(level, x, z, ground.getY() + 8, 16);
			if (height != NO_GROUND && Math.abs(height - ground.getY()) >= SLOPE_MIN_HEIGHT_DIFFERENCE) {
				return true;
			}
		}
		return false;
	}

	private static final int NO_GROUND = Integer.MIN_VALUE;

	/**
	 * Searches down from {@code fromY} for soil, passing through air, leaves and plants.
	 * Returns {@link #NO_GROUND} if it meets anything else first (water, logs, rocks) or finds nothing.
	 */
	private static int findGround(WorldGenLevel level, int x, int z, int fromY, int depth) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
		for (int i = 0; i <= depth; i++, pos.move(0, -1, 0)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.is(BlockTags.LEAVES) || isPlant(state)) {
				continue;
			}
			return isSoil(state) ? pos.getY() : NO_GROUND;
		}
		return NO_GROUND;
	}

	private static boolean isSoil(BlockState state) {
		return Feature.isDirt(state) || state.is(BlockTags.BASE_STONE_OVERWORLD);
	}

	private static boolean isAirOrPlant(BlockState state) {
		return state.isAir() || isPlant(state);
	}

	/** Small ground vegetation a tree may grow through. Fluids never count. */
	public static boolean isPlant(BlockState state) {
		if (!state.getFluidState().isEmpty()) {
			return false;
		}
		return state.canBeReplaced()
				|| state.is(BlockTags.REPLACEABLE_BY_TREES) && !state.is(BlockTags.LEAVES)
				|| state.is(BlockTags.SMALL_FLOWERS)
				|| state.is(Blocks.MOSS_CARPET)
				|| state.is(Blocks.AZALEA)
				|| state.is(Blocks.FLOWERING_AZALEA)
				|| state.is(Blocks.BROWN_MUSHROOM)
				|| state.is(Blocks.RED_MUSHROOM)
				|| state.is(Blocks.SWEET_BERRY_BUSH);
	}

	/**
	 * The parts of a template that matter for placement, in template coordinates.
	 *
	 * @param logs    every log/wood block
	 * @param contact logs on the lowest log layer: the trunk base that has to stand on the ground
	 * @param pivot   trunk base block closest to the base's center; the tree turns around it
	 */
	private record Shape(List<StructureTemplate.StructureBlockInfo> logs, List<StructureTemplate.StructureBlockInfo> contact,
			BlockPos pivot) {

		static Shape of(StructureTemplate template) {
			List<StructureTemplate.StructureBlockInfo> logs = new ArrayList<>();
			StructurePlaceSettings identity = new StructurePlaceSettings();
			for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.LOGS)) {
				logs.addAll(template.filterBlocks(BlockPos.ZERO, identity, block.value()));
			}
			if (logs.isEmpty()) {
				return new Shape(List.of(), List.of(), BlockPos.ZERO);
			}

			int minY = logs.stream().mapToInt(info -> info.pos().getY()).min().orElseThrow();
			List<StructureTemplate.StructureBlockInfo> contact = logs.stream().filter(info -> info.pos().getY() == minY).toList();
			double centerX = contact.stream().mapToInt(info -> info.pos().getX()).average().orElseThrow();
			double centerZ = contact.stream().mapToInt(info -> info.pos().getZ()).average().orElseThrow();
			BlockPos pivot = contact.get(0).pos();
			double best = Double.MAX_VALUE;
			for (StructureTemplate.StructureBlockInfo info : contact) {
				double dx = info.pos().getX() - centerX;
				double dz = info.pos().getZ() - centerZ;
				if (dx * dx + dz * dz < best) {
					best = dx * dx + dz * dz;
					pivot = info.pos();
				}
			}
			return new Shape(List.copyOf(logs), contact, pivot);
		}
	}
}
