package net.valor.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/** Ground lookups around the player for the Whispering Woods ambience (mist and glimmers). */
public final class WhisperingWoodsTerrain {
	public static final int NO_GROUND = Integer.MIN_VALUE;

	/** Ground is searched from this far above to this far below the reference height. */
	private static final int SEARCH_UP = 8;
	private static final int SEARCH_DOWN = 16;
	/** How far apart ground heights are compared to find hollows. */
	private static final int HOLLOW_PROBE_DISTANCE = 6;

	private WhisperingWoodsTerrain() {}

	/**
	 * Y of the first free block above the ground in this column, searched around {@code referenceY}.
	 * Leaves and plants are passed through; water counts as ground (mist floats on streams).
	 */
	public static int findGround(ClientLevel level, int x, int z, int referenceY) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, referenceY + SEARCH_UP, z);
		for (int i = 0; i <= SEARCH_UP + SEARCH_DOWN; i++, pos.move(0, -1, 0)) {
			BlockState state = level.getBlockState(pos);
			boolean ground = !state.getFluidState().isEmpty() || state.blocksMotion() && !state.is(BlockTags.LEAVES);
			if (ground) {
				return i == 0 ? NO_GROUND : pos.getY() + 1;
			}
		}
		return NO_GROUND;
	}

	/** 0 on flat ground or hills, up to 1 in a hollow about three blocks below its surroundings. */
	public static float hollowness(ClientLevel level, int x, int z, int ground) {
		int sum = 0;
		int count = 0;
		int[][] offsets = {{HOLLOW_PROBE_DISTANCE, 0}, {-HOLLOW_PROBE_DISTANCE, 0}, {0, HOLLOW_PROBE_DISTANCE}, {0, -HOLLOW_PROBE_DISTANCE}};
		for (int[] offset : offsets) {
			int around = findGround(level, x + offset[0], z + offset[1], ground);
			if (around != NO_GROUND) {
				sum += around;
				count++;
			}
		}
		if (count == 0) {
			return 0.0F;
		}
		return Mth.clamp((sum / (float) count - ground) / 3.0F, 0.0F, 1.0F);
	}
}
