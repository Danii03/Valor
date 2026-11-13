package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Soil scattered over the grass around a feature, e.g. podzol and coarse dirt around old wood.
 *
 * @param state  block placed instead of a grass block
 * @param radius how far around the feature's footprint, in blocks
 * @param chance chance per grass block right next to the footprint; thins out towards {@code radius}
 */
public record GroundCover(BlockStateProvider state, int radius, float chance) {

	public static final Codec<GroundCover> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockStateProvider.CODEC.fieldOf("state").forGetter(GroundCover::state),
			Codec.intRange(1, 6).fieldOf("radius").forGetter(GroundCover::radius),
			Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(GroundCover::chance)
	).apply(instance, GroundCover::new));
}
