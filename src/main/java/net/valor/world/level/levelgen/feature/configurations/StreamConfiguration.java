package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.StreamFeature}.
 *
 * @param length        number of one-block steps the stream meanders (it may end earlier where the ground does not suit it)
 * @param maxBankHeight how far the ground along the stream may rise above the water; steeper ground ends the stream
 * @param bankWidth     how far from the water the banks slope down towards it
 * @param wideChance    how often the stream widens into a small pool (5–6 blocks)
 */
public record StreamConfiguration(IntProvider length, int maxBankHeight, int bankWidth, float wideChance) implements FeatureConfiguration {

	public static final Codec<StreamConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			IntProvider.codec(8, 64).fieldOf("length").forGetter(StreamConfiguration::length),
			Codec.intRange(1, 6).fieldOf("max_bank_height").forGetter(StreamConfiguration::maxBankHeight),
			Codec.intRange(1, 6).fieldOf("bank_width").forGetter(StreamConfiguration::bankWidth),
			Codec.floatRange(0.0F, 1.0F).fieldOf("wide_chance").forGetter(StreamConfiguration::wideChance)
	).apply(instance, StreamConfiguration::new));
}
