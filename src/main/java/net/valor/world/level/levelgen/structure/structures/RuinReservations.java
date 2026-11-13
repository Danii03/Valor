package net.valor.world.level.levelgen.structure.structures;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Lets features keep their distance from {@link RuinStructure ruins}, whichever is generated first.
 * <p>
 * Structure starts (with their bounding boxes, which include the ruin's terrain blending) are decided in
 * the {@code structure_starts} chunk status, long before any feature runs, and a feature may read the starts
 * of all chunks up to 8 chunks away. So a tree decorating its chunk already knows about a ruin that will only
 * be built in the next chunk over, and can stay clear of it.
 */
public final class RuinReservations {
	/** Chebyshev distance (chunks) at which a generating chunk may still read structure starts. */
	private static final int READABLE_START_RADIUS = 8;

	private RuinReservations() {}

	/** Whether a ruin (including its blended surroundings) lies within {@code margin} blocks of the column at x/z. */
	public static boolean isNearRuin(WorldGenLevel level, int x, int z, int margin) {
		BoundingBox area = new BoundingBox(x - margin, level.getMinBuildHeight(), z - margin, x + margin, level.getMaxBuildHeight(), z + margin);
		// A ruin's start chunk lies at most its own size (plus blending) away from the parts it covers.
		int reach = margin + RuinPiece.MAX_HORIZONTAL_EXTENT;
		int minChunkX = SectionPos.blockToSectionCoord(x - reach);
		int maxChunkX = SectionPos.blockToSectionCoord(x + reach);
		int minChunkZ = SectionPos.blockToSectionCoord(z - reach);
		int maxChunkZ = SectionPos.blockToSectionCoord(z + reach);
		ChunkPos center = level instanceof WorldGenRegion region ? region.getCenter() : null;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (center != null && center.getChessboardDistance(chunkX, chunkZ) > READABLE_START_RADIUS) {
					continue;
				}
				ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false);
				if (chunk == null) {
					continue;
				}
				for (StructureStart start : chunk.getAllStarts().values()) {
					if (start.isValid() && start.getStructure() instanceof RuinStructure && start.getBoundingBox().intersects(area)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
