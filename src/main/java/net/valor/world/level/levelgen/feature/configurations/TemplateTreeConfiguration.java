package net.valor.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of {@link net.valor.world.level.levelgen.feature.TemplateTreeFeature}.
 *
 * @param template              structure template ({@code data/<ns>/structure/<path>.nbt}) of the tree
 * @param maxGroundStep         largest height difference allowed under the trunk; the gap on the
 *                              low side is filled with trunk blocks (roots), so trees never float
 * @param requiresSlopeOrWater  only grow on a hillside or near water (for characterful leaning trees)
 * @param clearance             if above 0, no other tree's logs (and no rocks or deadwood at the trunk's
 *                              foot) may be within the tree's outline widened by this many blocks;
 *                              for landmark trees that should stand on their own
 * @param blendTerrain          fill soil under the root flare and raise a gentle mound around the trunk's
 *                              foot, so a big tree looks rooted in the slope instead of set onto it
 * @param groundCover           soil scattered around the trunk's foot (podzol, coarse dirt, …)
 */
public record TemplateTreeConfiguration(ResourceLocation template, int maxGroundStep, boolean requiresSlopeOrWater,
		int clearance, boolean blendTerrain, Optional<GroundCover> groundCover) implements FeatureConfiguration {

	public static final Codec<TemplateTreeConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("template").forGetter(TemplateTreeConfiguration::template),
			Codec.intRange(0, 8).fieldOf("max_ground_step").forGetter(TemplateTreeConfiguration::maxGroundStep),
			Codec.BOOL.optionalFieldOf("requires_slope_or_water", false).forGetter(TemplateTreeConfiguration::requiresSlopeOrWater),
			Codec.intRange(0, 6).optionalFieldOf("clearance", 0).forGetter(TemplateTreeConfiguration::clearance),
			Codec.BOOL.optionalFieldOf("blend_terrain", false).forGetter(TemplateTreeConfiguration::blendTerrain),
			GroundCover.CODEC.optionalFieldOf("ground_cover").forGetter(TemplateTreeConfiguration::groundCover)
	).apply(instance, TemplateTreeConfiguration::new));

	/** A plain template tree, as used for the regular Whispering Woods trees. */
	public TemplateTreeConfiguration(ResourceLocation template, int maxGroundStep, boolean requiresSlopeOrWater) {
		this(template, maxGroundStep, requiresSlopeOrWater, 0, false, Optional.empty());
	}
}
