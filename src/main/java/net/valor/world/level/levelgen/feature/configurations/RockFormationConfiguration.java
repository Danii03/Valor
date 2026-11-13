package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.RockFormationFeature}.
 *
 * @param rock              block of a boulder's body
 * @param topRock           block of a boulder's exposed top, where moss settles first
 * @param boulders          number of boulders in the group (the first one is the largest)
 * @param radius            horizontal radius of the first boulder
 * @param spread            how far the other boulders lie from the first one
 * @param flatGroundChance  chance to generate on flat ground; hillsides and stream banks always qualify
 * @param mossChance        chance for a moss carpet on each exposed top block
 */
public record RockFormationConfiguration(BlockStateProvider rock, BlockStateProvider topRock, IntProvider boulders,
		FloatProvider radius, int spread, float flatGroundChance, float mossChance) implements FeatureConfiguration {

	public static final Codec<RockFormationConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BlockStateProvider.CODEC.fieldOf("rock").forGetter(RockFormationConfiguration::rock),
			BlockStateProvider.CODEC.fieldOf("top_rock").forGetter(RockFormationConfiguration::topRock),
			IntProvider.codec(1, 6).fieldOf("boulders").forGetter(RockFormationConfiguration::boulders),
			FloatProvider.codec(0.5F, 3.0F).fieldOf("radius").forGetter(RockFormationConfiguration::radius),
			Codec.intRange(0, 5).fieldOf("spread").forGetter(RockFormationConfiguration::spread),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flat_ground_chance", 1.0F).forGetter(RockFormationConfiguration::flatGroundChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("moss_chance", 0.0F).forGetter(RockFormationConfiguration::mossChance)
	).apply(instance, RockFormationConfiguration::new));
}
