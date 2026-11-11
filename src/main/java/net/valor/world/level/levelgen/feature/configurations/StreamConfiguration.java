package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.StreamFeature}.
 *
 * @param length        number of steps the stream meanders
 * @param maxBankHeight deepest the stream may cut into the terrain; on steeper ground no stream is placed
 * @param wideChance    chance per step that the stream is two blocks wide there
 */
public record StreamConfiguration(IntProvider length, int maxBankHeight, float wideChance) implements FeatureConfiguration {

	public static final Codec<StreamConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			IntProvider.codec(4, 32).fieldOf("length").forGetter(StreamConfiguration::length),
			Codec.intRange(1, 6).fieldOf("max_bank_height").forGetter(StreamConfiguration::maxBankHeight),
			Codec.floatRange(0.0F, 1.0F).fieldOf("wide_chance").forGetter(StreamConfiguration::wideChance)
	).apply(instance, StreamConfiguration::new));
}
