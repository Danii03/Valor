package net.valor.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.valor.registry.ValorStructureTypes;
import net.valor.world.level.biome.ValorBiomes;
import net.valor.world.level.levelgen.WhisperingWoodsZones;

/**
 * A small, hand-built ruin ({@code .nbt}) lying in the forest for centuries, with a dig site around it.
 * <p>
 * Where and how often ruins appear is decided by the structure set (vanilla {@code random_spread}: one
 * candidate per cell of chunks, weighted choice between the ruin types). This class decides whether a
 * candidate spot is good enough, using the terrain as the noise generator shapes it, before any feature
 * runs. A spot is skipped rather than forced:
 * <ul>
 *     <li>the whole footprint and its surroundings must be dry land, inside the biome;</li>
 *     <li>the ground under the footprint may vary by at most {@link #maxGroundStep} blocks, and its
 *     surroundings must not drop away (no cliffs, no hill crests for ruins that prefer hollows);</li>
 *     <li>the Whispering Woods zone at the spot (Outer Woods, Deepwood, Hidden Grove) gives a chance to keep
 *     it, so each ruin type favors the parts of the forest that suit it.</li>
 * </ul>
 * The ruin rests at the median ground height and is sunk {@link #sink} blocks into it. How it is fitted
 * into the terrain is up to {@link RuinPiece}. Trees and other features keep their distance through
 * {@link RuinReservations}.
 */
public final class RuinStructure extends Structure {

	public static final MapCodec<RuinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			settingsCodec(instance),
			ResourceLocation.CODEC.fieldOf("template").forGetter(structure -> structure.template),
			IntProvider.codec(0, 4).fieldOf("sink").forGetter(structure -> structure.sink),
			Codec.intRange(0, 6).fieldOf("max_ground_step").forGetter(structure -> structure.maxGroundStep),
			Codec.BOOL.optionalFieldOf("prefer_hollow", false).forGetter(structure -> structure.preferHollow),
			ZoneChance.CODEC.fieldOf("zone_chance").forGetter(structure -> structure.zoneChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("moss_chance", 0.0F).forGetter(structure -> structure.mossChance),
			RuinArchaeology.CODEC.fieldOf("archaeology").forGetter(structure -> structure.archaeology)
	).apply(instance, RuinStructure::new));

	/** Distance between terrain samples under the footprint. */
	private static final int SAMPLE_STEP = 2;
	/** How far around the footprint the surroundings are checked. */
	private static final int SURROUNDING_DISTANCE = 3;
	/** The surroundings may drop or rise this much more than the footprint itself. */
	private static final int SURROUNDING_EXTRA_STEP = 3;

	private final ResourceLocation template;
	private final IntProvider sink;
	private final int maxGroundStep;
	private final boolean preferHollow;
	private final ZoneChance zoneChance;
	private final float mossChance;
	private final RuinArchaeology archaeology;

	public RuinStructure(StructureSettings settings, ResourceLocation template, IntProvider sink, int maxGroundStep,
			boolean preferHollow, ZoneChance zoneChance, float mossChance, RuinArchaeology archaeology) {
		super(settings);
		this.template = template;
		this.sink = sink;
		this.maxGroundStep = maxGroundStep;
		this.preferHollow = preferHollow;
		this.zoneChance = zoneChance;
		this.mossChance = mossChance;
		this.archaeology = archaeology;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		StructureTemplate structureTemplate = context.structureTemplateManager().getOrCreate(this.template);
		Vec3i size = structureTemplate.getSize();
		if (size.getX() == 0 || size.getZ() == 0) {
			return Optional.empty();
		}

		Rotation rotation = Rotation.getRandom(random);
		Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
		ChunkPos chunk = context.chunkPos();
		int centerX = chunk.getBlockX(random.nextInt(16));
		int centerZ = chunk.getBlockZ(random.nextInt(16));
		// Cheap check first: vanilla only tests the biome after this method, but candidates exist all over the world.
		int centerGround = dryGround(context, centerX, centerZ);
		if (centerGround == Integer.MIN_VALUE || !isValidBiomeAt(context, centerX, centerGround, centerZ)) {
			return Optional.empty();
		}
		BlockPos pivot = RuinPiece.pivot(size);
		BlockPos corner = new BlockPos(centerX - pivot.getX(), 0, centerZ - pivot.getZ());
		BoundingBox box = structureTemplate.getBoundingBox(RuinPiece.makeSettings(rotation, mirror, pivot), corner);

		// Ground under the footprint: dry, and gentle enough.
		List<Integer> footprint = new ArrayList<>();
		for (int x = box.minX(); x <= box.maxX(); x = step(x, box.maxX())) {
			for (int z = box.minZ(); z <= box.maxZ(); z = step(z, box.maxZ())) {
				int ground = dryGround(context, x, z);
				if (ground == Integer.MIN_VALUE) {
					return Optional.empty();
				}
				footprint.add(ground);
			}
		}
		footprint.sort(null);
		int lowest = footprint.get(0);
		int highest = footprint.get(footprint.size() - 1);
		if (highest - lowest > this.maxGroundStep) {
			return Optional.empty();
		}
		int restY = footprint.get(footprint.size() / 2);
		double footprintMean = footprint.stream().mapToInt(Integer::intValue).average().orElse(restY);

		// Surroundings: dry land in the biome, no cliff right next to the ruin, no crest for hollow-loving ruins.
		BoundingBox around = box.inflatedBy(SURROUNDING_DISTANCE, 0, SURROUNDING_DISTANCE);
		int sum = 0;
		int count = 0;
		for (int[] point : perimeter(around)) {
			int ground = dryGround(context, point[0], point[1]);
			if (ground == Integer.MIN_VALUE || Math.abs(ground - restY) > this.maxGroundStep + SURROUNDING_EXTRA_STEP) {
				return Optional.empty();
			}
			if (!isValidBiomeAt(context, point[0], ground, point[1])) {
				return Optional.empty();
			}
			sum += ground;
			count++;
		}
		if (this.preferHollow && footprintMean > sum / (double) count + 0.5) {
			return Optional.empty();
		}

		// Each ruin type has its favorite parts of the forest.
		BlockPos center = new BlockPos(centerX, restY, centerZ);
		WhisperingWoodsZones.Zone zone = WhisperingWoodsZones.sample(context.seed(), (qx, qy, qz) -> context.biomeSource()
				.getNoiseBiome(qx, qy, qz, context.randomState().sampler()).is(ValorBiomes.WHISPERING_WOODS), center);
		if (random.nextFloat() >= zone.blend(this.zoneChance.outerWoods(), this.zoneChance.deepwood(), this.zoneChance.hiddenGrove())) {
			return Optional.empty();
		}

		int bottomY = restY + 1 - this.sink.sample(random);
		BlockPos position = corner.atY(bottomY);
		return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new RuinPiece(context.structureTemplateManager(),
				this.template, position, rotation, mirror, restY, this.mossChance, this.archaeology))));
	}

	/** Sample positions every {@link #SAMPLE_STEP} blocks, always including the last one. */
	private static int step(int value, int max) {
		return value == max ? max + 1 : Math.min(value + SAMPLE_STEP, max);
	}

	private static List<int[]> perimeter(BoundingBox box) {
		List<int[]> points = new ArrayList<>();
		for (int x = box.minX(); x <= box.maxX(); x += SURROUNDING_DISTANCE) {
			points.add(new int[] {x, box.minZ()});
			points.add(new int[] {x, box.maxZ()});
		}
		for (int z = box.minZ() + SURROUNDING_DISTANCE; z < box.maxZ(); z += SURROUNDING_DISTANCE) {
			points.add(new int[] {box.minX(), z});
			points.add(new int[] {box.maxX(), z});
		}
		return points;
	}

	private static boolean isValidBiomeAt(GenerationContext context, int x, int y, int z) {
		return context.validBiome().test(context.biomeSource().getNoiseBiome(
				QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler()));
	}

	/** Height of the topmost terrain block, or {@link Integer#MIN_VALUE} if water (or another fluid) covers it. */
	private static int dryGround(GenerationContext context, int x, int z) {
		int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
		return surface == floor ? surface - 1 : Integer.MIN_VALUE;
	}

	@Override
	public StructureType<?> type() {
		return ValorStructureTypes.RUIN;
	}

	/**
	 * Chance to keep a candidate spot in each Whispering Woods zone. The zones blend smoothly.
	 */
	public record ZoneChance(float outerWoods, float deepwood, float hiddenGrove) {
		public static final Codec<ZoneChance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.floatRange(0.0F, 1.0F).fieldOf("outer_woods").forGetter(ZoneChance::outerWoods),
				Codec.floatRange(0.0F, 1.0F).fieldOf("deepwood").forGetter(ZoneChance::deepwood),
				Codec.floatRange(0.0F, 1.0F).fieldOf("hidden_grove").forGetter(ZoneChance::hiddenGrove)
		).apply(instance, ZoneChance::new));
	}
}
