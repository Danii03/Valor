package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.GroundTemplateFeature}.
 *
 * @param template          structure template ({@code data/<ns>/structure/<path>.nbt})
 * @param maxGroundStep     largest height difference allowed under the whole footprint; steeper spots are skipped
 * @param sinkChance        chance to sink the template one block into the ground, as if settled over time
 * @param clearance         no logs may be within the template's outline widened by this many blocks
 * @param flatGroundChance  chance to generate on flat ground; hillsides and stream banks always qualify,
 *                          so lower values favor them
 * @param mossChance        chance for a moss carpet on each exposed top of the template's wood
 * @param groundCover       soil scattered around the footprint
 */
public record GroundTemplateConfiguration(ResourceLocation template, int maxGroundStep, float sinkChance, int clearance,
		float flatGroundChance, float mossChance, Optional<GroundCover> groundCover) implements FeatureConfiguration {

	public static final Codec<GroundTemplateConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("template").forGetter(GroundTemplateConfiguration::template),
			Codec.intRange(0, 4).fieldOf("max_ground_step").forGetter(GroundTemplateConfiguration::maxGroundStep),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("sink_chance", 0.0F).forGetter(GroundTemplateConfiguration::sinkChance),
			Codec.intRange(0, 4).optionalFieldOf("clearance", 1).forGetter(GroundTemplateConfiguration::clearance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("flat_ground_chance", 1.0F).forGetter(GroundTemplateConfiguration::flatGroundChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("moss_chance", 0.0F).forGetter(GroundTemplateConfiguration::mossChance),
			GroundCover.CODEC.optionalFieldOf("ground_cover").forGetter(GroundTemplateConfiguration::groundCover)
	).apply(instance, GroundTemplateConfiguration::new));
}
