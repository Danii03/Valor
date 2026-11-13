package net.valor.world.level.levelgen.structure.structures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.valor.registry.ValorStructurePieceTypes;
import net.valor.world.level.levelgen.feature.TerrainFitting;
import net.valor.world.level.levelgen.structure.templatesystem.RuinProcessor;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateBlocks;

/**
 * Places a {@link RuinStructure ruin} and settles it into the actual terrain, chunk by chunk:
 * <ol>
 *     <li><b>Foundation</b>: where the ground under a wall is lower than the wall's base, soil is filled in
 *     up to it, so nothing floats and no foundation is cut off. Never more than {@link #MAX_FILL} blocks.</li>
 *     <li><b>Blending</b>: that soil continues as a gentle mound into the surroundings, falling one block
 *     per block of distance with a little roughness, so there are no hard square edges.</li>
 *     <li><b>Ruin</b>: air in the template is ignored, so the ruin never carves terrain; sunk walls stand in
 *     the soil and sunken rooms stay filled with earth (see {@link RuinProcessor}).</li>
 *     <li><b>Dig site</b>: frayed patches of rubble-strewn ground and a few blocks of suspicious gravel with
 *     vanilla archaeology loot, mostly at the foot of walls and inside sunken rooms (see {@link RuinArchaeology}).</li>
 *     <li><b>Overgrowth</b>: some moss on the tops of the stones; grass and ferns grow around and inside the
 *     ruin afterwards, since vegetation is placed after structures.</li>
 * </ol>
 * The piece's bounding box includes the blended surroundings, so neighboring chunks blend their part too,
 * and so {@link RuinReservations} keeps trees and other features clear of all of it.
 * <p>
 * Everything that spans several chunks (patches, suspicious gravel spots) is planned from the ruin's own
 * position and the world seed, so every chunk sees the same plan and applies its share.
 */
public class RuinPiece extends TemplateStructurePiece {
	/** How far the blending and the dig site reach beyond the ruin itself. */
	public static final int BLEND_RADIUS = 3;
	/** Upper bound of how far a ruin piece, with blending, reaches beyond its start chunk. */
	public static final int MAX_HORIZONTAL_EXTENT = 24;
	private static final int MAX_FILL = 4;
	private static final int MOUND_MAX_RAISE = 2;
	/** The dig site only uses ground within this many blocks of the ruin's resting height. */
	private static final int DIG_SITE_HEIGHT_TOLERANCE = 2;
	/** Suspicious gravel lies at most this far from the ruin (Chebyshev distance, blocks). */
	private static final int SUSPICIOUS_MAX_DISTANCE = 2;

	private final Rotation rotation;
	private final Mirror mirror;
	private final int restY;
	private final float mossChance;
	private final RuinArchaeology archaeology;
	private Map<Long, Integer> footprint;

	public RuinPiece(StructureTemplateManager manager, ResourceLocation template, BlockPos position, Rotation rotation, Mirror mirror,
			int restY, float mossChance, RuinArchaeology archaeology) {
		super(ValorStructurePieceTypes.RUIN, 0, manager, template, template.toString(),
				makeSettings(rotation, mirror, pivot(manager.getOrCreate(template).getSize())), position);
		this.rotation = rotation;
		this.mirror = mirror;
		this.restY = restY;
		this.mossChance = mossChance;
		this.archaeology = archaeology;
		this.boundingBox = this.blendedBox();
	}

	public RuinPiece(StructureTemplateManager manager, CompoundTag tag) {
		super(ValorStructurePieceTypes.RUIN, tag, manager, id -> makeSettings(Rotation.valueOf(tag.getString("Rot")),
				Mirror.valueOf(tag.getString("Mirror")), pivot(manager.getOrCreate(id).getSize())));
		this.rotation = Rotation.valueOf(tag.getString("Rot"));
		this.mirror = Mirror.valueOf(tag.getString("Mirror"));
		this.restY = tag.getInt("RestY");
		this.mossChance = tag.getFloat("Moss");
		this.archaeology = RuinArchaeology.CODEC.parse(NbtOps.INSTANCE, tag.get("Archaeology")).getOrThrow();
		this.boundingBox = this.blendedBox();
	}

	/** Ruins turn around the middle of their footprint. */
	static BlockPos pivot(Vec3i size) {
		return new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
	}

	static StructurePlaceSettings makeSettings(Rotation rotation, Mirror mirror, BlockPos pivot) {
		return new StructurePlaceSettings()
				.setRotation(rotation)
				.setMirror(mirror)
				.setRotationPivot(pivot)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
				.addProcessor(RuinProcessor.INSTANCE);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putString("Rot", this.rotation.name());
		tag.putString("Mirror", this.mirror.name());
		tag.putInt("RestY", this.restY);
		tag.putFloat("Moss", this.mossChance);
		tag.put("Archaeology", RuinArchaeology.CODEC.encodeStart(NbtOps.INSTANCE, this.archaeology).getOrThrow());
	}

	@Override
	protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
	}

	private BoundingBox templateBox() {
		return this.template.getBoundingBox(this.placeSettings, this.templatePosition);
	}

	private BoundingBox blendedBox() {
		BoundingBox box = this.templateBox();
		return new BoundingBox(box.minX() - BLEND_RADIUS, box.minY() - MAX_FILL, box.minZ() - BLEND_RADIUS,
				box.maxX() + BLEND_RADIUS, box.maxY(), box.maxZ() + BLEND_RADIUS);
	}

	/**
	 * The columns where the ruin stands on the ground: solid blocks of the template's lowest layer, mapped to
	 * their world height ({@link BlockPos#asLong} of x/0/z). Overhangs and hanging vines are not part of it,
	 * so no soil is ever piled up under them.
	 */
	private Map<Long, Integer> footprint() {
		if (this.footprint == null) {
			List<StructureTemplate.StructureBlockInfo> blocks = TemplateBlocks.of(this.template);
			int lowestLayer = blocks.stream().mapToInt(info -> info.pos().getY()).min().orElse(0);
			Map<Long, Integer> columns = new HashMap<>();
			for (StructureTemplate.StructureBlockInfo info : blocks) {
				if (info.pos().getY() != lowestLayer || info.state().canBeReplaced()) {
					continue;
				}
				BlockPos pos = this.worldPos(info.pos());
				columns.put(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY());
			}
			this.footprint = columns;
		}
		return this.footprint;
	}

	private BlockPos worldPos(BlockPos templatePos) {
		return StructureTemplate.transform(templatePos, this.mirror, this.rotation, this.placeSettings.getRotationPivot()).offset(this.templatePosition);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
		Map<Long, Integer> footprint = this.footprint();

		// 1. Foundation: soil up to the base of every wall in this chunk.
		Map<Long, Integer> groundUnderRuin = new HashMap<>();
		for (Map.Entry<Long, Integer> entry : footprint.entrySet()) {
			BlockPos column = BlockPos.of(entry.getKey());
			int base = entry.getValue();
			groundUnderRuin.put(entry.getKey(), base - 1);
			if (!chunkBox.isInside(column.getX(), chunkBox.minY(), column.getZ())) {
				continue;
			}
			int ground = TerrainFitting.findGround(level, column.getX(), column.getZ(), base + 1, MAX_FILL + 3);
			if (ground != TerrainFitting.NO_GROUND && ground < base - 1 && base - 1 - ground <= MAX_FILL) {
				TerrainFitting.fillSoil(level, column.getX(), column.getZ(), ground, base - 1);
			}
		}
		// 2. Blending: a gentle mound where soil had to be filled in.
		TerrainFitting.raiseSurroundings(level, random, groundUnderRuin, BLEND_RADIUS, MOUND_MAX_RAISE, chunkBox);

		// 3. The ruin itself.
		super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pivot);
		BoundingBox ruinBox = this.templateBox();
		this.boundingBox = this.blendedBox();

		// 4. Dig site, 5. moss.
		this.placeDigSite(level, chunkBox, footprint);
		this.growMoss(level, random, chunkBox);
		TerrainFitting.removeUnsupportedPlants(level, TerrainFitting.clamp(new BoundingBox(
				ruinBox.minX() - BLEND_RADIUS - 1, this.restY - MAX_FILL - 2, ruinBox.minZ() - BLEND_RADIUS - 1,
				ruinBox.maxX() + BLEND_RADIUS + 1, ruinBox.maxY() + 2, ruinBox.maxZ() + BLEND_RADIUS + 1), chunkBox));
	}

	private void placeDigSite(WorldGenLevel level, BoundingBox chunkBox, Map<Long, Integer> footprint) {
		RandomSource plan = RandomSource.create(level.getSeed() ^ Mth.getSeed(this.templatePosition));

		// Ground around the ruin (and inside sunken rooms), nearest first after shuffling.
		BoundingBox ruinBox = this.templateBox();
		List<long[]> spots = new ArrayList<>();
		for (int x = ruinBox.minX() - BLEND_RADIUS; x <= ruinBox.maxX() + BLEND_RADIUS; x++) {
			for (int z = ruinBox.minZ() - BLEND_RADIUS; z <= ruinBox.maxZ() + BLEND_RADIUS; z++) {
				if (!footprint.containsKey(BlockPos.asLong(x, 0, z))) {
					int distance = distanceToRuin(footprint, x, z);
					if (distance <= BLEND_RADIUS) {
						spots.add(new long[] {x, z, distance});
					}
				}
			}
		}
		if (spots.isEmpty()) {
			return;
		}
		Util.shuffle(spots, plan);

		// Patches of rubble-strewn ground: frayed edges, different sizes.
		int patchCount = this.archaeology.patches().sample(plan);
		List<float[]> patches = new ArrayList<>();
		for (int i = 0; i < patchCount && i < spots.size(); i++) {
			patches.add(new float[] {spots.get(i)[0], spots.get(i)[1], this.archaeology.patchRadius().sample(plan)});
		}
		for (int x = ruinBox.minX() - BLEND_RADIUS - 3; x <= ruinBox.maxX() + BLEND_RADIUS + 3; x++) {
			for (int z = ruinBox.minZ() - BLEND_RADIUS - 3; z <= ruinBox.maxZ() + BLEND_RADIUS + 3; z++) {
				if (!chunkBox.isInside(x, chunkBox.minY(), z) || footprint.containsKey(BlockPos.asLong(x, 0, z)) || !inPatch(patches, x, z)) {
					continue;
				}
				int ground = this.digSiteGround(level, x, z);
				if (ground == Integer.MIN_VALUE) {
					continue;
				}
				BlockPos pos = new BlockPos(x, ground, z);
				BlockState state = this.archaeology.ground().getState(RandomSource.create(level.getSeed() ^ pos.asLong()), pos);
				if (level.getBlockState(pos).is(BlockTags.DIRT) && isSupported(level, pos)) {
					level.setBlock(pos, state, Block.UPDATE_CLIENTS);
				}
			}
		}

		// Suspicious gravel at the foot of walls and inside sunken rooms.
		List<long[]> near = new ArrayList<>(spots.stream().filter(spot -> spot[2] <= SUSPICIOUS_MAX_DISTANCE).toList());
		near.sort(Comparator.comparingLong(spot -> spot[2] > 1 ? 1 : 0));
		int suspicious = this.archaeology.suspicious().sample(plan);
		for (int i = 0; i < suspicious && i < near.size(); i++) {
			long[] spot = near.get(i);
			boolean rare = plan.nextFloat() < this.archaeology.rareChance();
			boolean buried = plan.nextFloat() < this.archaeology.buriedChance();
			long lootSeed = plan.nextLong();
			int x = (int) spot[0];
			int z = (int) spot[1];
			if (!chunkBox.isInside(x, chunkBox.minY(), z)) {
				continue;
			}
			int ground = this.digSiteGround(level, x, z);
			if (ground == Integer.MIN_VALUE) {
				continue;
			}
			BlockPos pos = new BlockPos(x, buried ? ground - 1 : ground, z);
			if (!isSupported(level, pos) || !isDiggable(level.getBlockState(pos))) {
				continue;
			}
			if (buried && level.getBlockState(pos.above()).is(Blocks.GRASS_BLOCK)) {
				level.setBlock(pos.above(), Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			level.setBlock(pos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
			if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
				brushable.setLootTable(rare ? this.archaeology.rareLoot() : this.archaeology.commonLoot(), lootSeed);
			}
		}
	}

	/** The exposed ground at x/z near the ruin's resting height, or {@link Integer#MIN_VALUE}. */
	private int digSiteGround(WorldGenLevel level, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, this.restY + DIG_SITE_HEIGHT_TOLERANCE + 1, z);
		for (int i = 0; i <= 2 * DIG_SITE_HEIGHT_TOLERANCE + 1; i++, pos.move(Direction.DOWN)) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || TerrainFitting.isPlant(state)) {
				continue;
			}
			return isDiggable(state) ? pos.getY() : Integer.MIN_VALUE;
		}
		return Integer.MIN_VALUE;
	}

	/** Ground a dig site may use: soil and loose rubble, never the ruin's own stones. */
	private static boolean isDiggable(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE);
	}

	/** Gravel falls, so it needs solid ground beneath. */
	private static boolean isSupported(WorldGenLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	private static boolean inPatch(List<float[]> patches, int x, int z) {
		// A per-column jitter frays the edges, so patches are never round.
		float jitter = 0.7F + 0.6F * (int) ((Mth.getSeed(x, 0, z) >>> 16) & 0xFFFFL) / 65535.0F;
		for (float[] patch : patches) {
			float dx = x - patch[0];
			float dz = z - patch[1];
			if (dx * dx + dz * dz <= Mth.square(patch[2] * jitter)) {
				return true;
			}
		}
		return false;
	}

	private static int distanceToRuin(Map<Long, Integer> footprint, int x, int z) {
		int best = Integer.MAX_VALUE;
		for (long key : footprint.keySet()) {
			BlockPos column = BlockPos.of(key);
			best = Math.min(best, Math.max(Math.abs(column.getX() - x), Math.abs(column.getZ() - z)));
		}
		return best;
	}

	/** A little moss on the exposed tops of the stones. */
	private void growMoss(WorldGenLevel level, RandomSource random, BoundingBox chunkBox) {
		if (this.mossChance <= 0.0F) {
			return;
		}
		for (StructureTemplate.StructureBlockInfo info : TemplateBlocks.of(this.template)) {
			BlockPos pos = this.worldPos(info.pos());
			BlockPos above = pos.above();
			if (!chunkBox.isInside(above) || random.nextFloat() >= this.mossChance) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!state.is(BlockTags.LEAVES) && state.isFaceSturdy(level, pos, Direction.UP) && level.getBlockState(above).isAir()) {
				level.setBlock(above, Blocks.MOSS_CARPET.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}
}
