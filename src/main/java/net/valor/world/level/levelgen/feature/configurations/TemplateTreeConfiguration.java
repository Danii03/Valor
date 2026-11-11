package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.TemplateTreeFeature}.
 *
 * @param template              structure template ({@code data/<ns>/structure/<path>.nbt}) of the tree
 * @param maxGroundStep         largest height difference allowed under the trunk; the gap on the
 *                              low side is filled with trunk blocks (roots), so trees never float
 * @param requiresSlopeOrWater  only grow on a hillside or near water (for characterful leaning trees)
 */
public record TemplateTreeConfiguration(ResourceLocation template, int maxGroundStep, boolean requiresSlopeOrWater)
		implements FeatureConfiguration {

	public static final Codec<TemplateTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("template").forGetter(TemplateTreeConfiguration::template),
			Codec.intRange(0, 8).fieldOf("max_ground_step").forGetter(TemplateTreeConfiguration::maxGroundStep),
			Codec.BOOL.optionalFieldOf("requires_slope_or_water", false).forGetter(TemplateTreeConfiguration::requiresSlopeOrWater)
	).apply(instance, TemplateTreeConfiguration::new));
}
