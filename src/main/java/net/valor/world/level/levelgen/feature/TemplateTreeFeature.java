package net.valor.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import net.valor.world.level.levelgen.feature.configurations.TemplateTreeConfiguration;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateBlocks;
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
 * Landmark trees can additionally ask for {@linkplain TemplateTreeConfiguration#clearance() room of their own}
 * and for {@linkplain TemplateTreeConfiguration#blendTerrain() terrain blending}: the decorative root blocks
 * at the trunk's foot get soil beneath them, the ground around the foot rises into a gentle mound, and grass
 * and ferns grow back on it.
 * <p>
 * Expected to run after the ground vegetation, so undergrowth also grows under canopies.
 */
public class TemplateTreeFeature extends Feature<TemplateTreeConfiguration> {
	/** How far down the trunk position is searched for soil (through canopies of earlier trees). */
	private static final int MAX_GROUND_SEARCH = 40;
	/** At most this fraction of the logs may end up inside terrain (hillsides); more means a cliff or overhang. */
	private static final float MAX_EMBEDDED_LOG_FRACTION = 0.2F;
	/** Terrain blending: how far the mound around the trunk's foot reaches, and how much it may raise the ground. */
	private static final int MOUND_RADIUS = 3;
	private static final int MOUND_MAX_RAISE = 2;
	private static final float MOUND_REGROWTH_CHANCE = 0.45F;
	/** Clearance: rocks and deadwood are only looked for this high above the base, where they could touch the trunk. */
	private static final int CLEARANCE_GROUND_HEIGHT = 4;

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

		int pivotGround = TerrainFitting.findGround(level, origin.getX(), origin.getZ(), origin.getY() + 1, MAX_GROUND_SEARCH);
		if (pivotGround == TerrainFitting.NO_GROUND) {
			return false;
		}
		if (config.requiresSlopeOrWater() && !TerrainFitting.isNearSlopeOrWater(level, new BlockPos(origin.getX(), pivotGround, origin.getZ()))) {
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
			int ground = TerrainFitting.findGround(level, x, z, pivotGround + step + 1, 2 * step + 2);
			if (ground == TerrainFitting.NO_GROUND || ground == pivotGround + step + 1) {
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

		// Terrain blending: the decorative roots (stairs, slabs) beside the trunk need ground under them too.
		List<BlockPos> footingWorld = new ArrayList<>();
		int[] footingGround = new int[shape.footing().size()];
		if (config.blendTerrain()) {
			for (int i = 0; i < shape.footing().size(); i++) {
				BlockPos local = StructureTemplate.transform(shape.footing().get(i), mirror, rotation, pivot).subtract(rotatedPivot);
				int x = origin.getX() + local.getX();
				int z = origin.getZ() + local.getZ();
				int ground = TerrainFitting.findGround(level, x, z, highestGround + 1, step + 2);
				if (ground == TerrainFitting.NO_GROUND || highestGround - ground > step) {
					return false; // a root would hang over water, a rock or a drop
				}
				footingGround[i] = ground;
				footingWorld.add(new BlockPos(x, 0, z));
				lowestGround = Math.min(lowestGround, ground);
			}
		}

		int baseY = highestGround + 1;
		BlockPos offset = new BlockPos(origin.getX() - rotatedPivot.getX(), baseY - rotatedPivot.getY(), origin.getZ() - rotatedPivot.getZ());

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

		BoundingBox treeBox = template.getBoundingBox(settings, offset);
		int margin = config.blendTerrain() ? MOUND_RADIUS + 1 : 0;
		if (!writable.isInside(treeBox.minX() - margin, treeBox.minY(), treeBox.minZ() - margin)
				|| !writable.isInside(treeBox.maxX() + margin, treeBox.maxY(), treeBox.maxZ() + margin)) {
			return false;
		}
		if (!hasRoomForTrunk(level, shape, offset, mirror, rotation, pivot)) {
			return false;
		}
		if (config.clearance() > 0 && !hasClearance(level, treeBox, baseY, config.clearance())) {
			return false;
		}

		// Roots on the low side of a slope, so the trunk never floats.
		Map<Long, Integer> footprint = new HashMap<>();
		for (int i = 0; i < contactWorld.size(); i++) {
			BlockState rootState = shape.contact().get(i).state().mirror(mirror).rotate(rotation);
			BlockPos column = contactWorld.get(i);
			for (int y = contactGround[i] + 1; y < baseY; y++) {
				BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
				if (TerrainFitting.isAirOrPlant(level.getBlockState(pos))) {
					level.setBlock(pos, rootState, Block.UPDATE_CLIENTS);
				}
			}
			BlockPos soil = new BlockPos(column.getX(), contactGround[i], column.getZ());
			BlockState soilState = level.getBlockState(soil);
			if (soilState.is(Blocks.GRASS_BLOCK) || soilState.is(Blocks.DIRT) || soilState.is(Blocks.PODZOL)) {
				level.setBlock(soil, Blocks.ROOTED_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			footprint.put(column.asLong(), baseY - 1);
		}

		List<BlockPos> newSurfaces = new ArrayList<>();
		if (config.blendTerrain()) {
			for (int i = 0; i < footingWorld.size(); i++) {
				BlockPos column = footingWorld.get(i);
				TerrainFitting.fillSoil(level, column.getX(), column.getZ(), footingGround[i], baseY - 1);
				footprint.put(column.asLong(), baseY - 1);
			}
			newSurfaces.addAll(TerrainFitting.raiseSurroundings(level, random, footprint, MOUND_RADIUS, MOUND_MAX_RAISE, writable));
		}

		boolean placed = template.placeInWorld(level, offset, offset, settings, random, Block.UPDATE_CLIENTS);

		if (config.blendTerrain() || config.groundCover().isPresent()) {
			config.groundCover().ifPresent(cover -> TerrainFitting.coverGround(level, random, footprint.keySet(), baseY + 1, cover, writable));
			TerrainFitting.revegetate(level, random, newSurfaces, MOUND_REGROWTH_CHANCE);
			int reach = MOUND_RADIUS + 1;
			TerrainFitting.removeUnsupportedPlants(level, TerrainFitting.clamp(new BoundingBox(
					treeBox.minX() - reach, lowestGround - 1, treeBox.minZ() - reach,
					treeBox.maxX() + reach, baseY + 2, treeBox.maxZ() + reach), writable));
		}
		return placed;
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
			if (!state.isAir() && !state.is(BlockTags.LEAVES) && !TerrainFitting.isPlant(state) && ++embedded > maxEmbedded) {
				return false;
			}
		}
		return true;
	}

	/**
	 * A landmark tree wants to stand on its own: no other tree's logs anywhere within its outline (widened by
	 * {@code clearance}), so no foreign trunk pokes through its crown, and no rocks or deadwood at its foot.
	 */
	private static boolean hasClearance(WorldGenLevel level, BoundingBox treeBox, int baseY, int clearance) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = treeBox.minX() - clearance; x <= treeBox.maxX() + clearance; x++) {
			for (int z = treeBox.minZ() - clearance; z <= treeBox.maxZ() + clearance; z++) {
				for (int y = baseY; y <= treeBox.maxY(); y++) {
					BlockState state = level.getBlockState(pos.set(x, y, z));
					if (state.is(BlockTags.LOGS)) {
						return false;
					}
					if (y < baseY + CLEARANCE_GROUND_HEIGHT && !state.isAir() && !state.is(BlockTags.LEAVES)
							&& !TerrainFitting.isPlant(state) && !TerrainFitting.isSoil(state)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * The parts of a template that matter for placement, in template coordinates.
	 *
	 * @param logs    every log/wood block
	 * @param contact logs on the lowest log layer: the trunk base that has to stand on the ground
	 * @param footing other blocks on that layer (stairs and slabs as root flares) that need ground beneath
	 * @param pivot   trunk base block closest to the base's center; the tree turns around it
	 */
	private record Shape(List<StructureTemplate.StructureBlockInfo> logs, List<StructureTemplate.StructureBlockInfo> contact,
			List<BlockPos> footing, BlockPos pivot) {

		static Shape of(StructureTemplate template) {
			List<StructureTemplate.StructureBlockInfo> logs = new ArrayList<>();
			StructurePlaceSettings identity = new StructurePlaceSettings();
			for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.LOGS)) {
				logs.addAll(template.filterBlocks(BlockPos.ZERO, identity, block.value()));
			}
			if (logs.isEmpty()) {
				return new Shape(List.of(), List.of(), List.of(), BlockPos.ZERO);
			}

			int minY = logs.stream().mapToInt(info -> info.pos().getY()).min().orElseThrow();
			List<StructureTemplate.StructureBlockInfo> contact = logs.stream().filter(info -> info.pos().getY() == minY).toList();
			List<BlockPos> footing = TemplateBlocks.of(template).stream()
					.filter(info -> info.pos().getY() == minY && !info.state().is(BlockTags.LOGS) && !info.state().is(BlockTags.LEAVES))
					.map(StructureTemplate.StructureBlockInfo::pos)
					.toList();
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
			return new Shape(List.copyOf(logs), contact, footing, pivot);
		}
	}
}
