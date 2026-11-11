package net.valor.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.valor.world.level.biome.ValorBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the Whispering Woods to the Overworld's biome parameter list the same way vanilla adds biome
 * variants, so the biome source, features, structures and {@code /locate} know about it.
 * <p>
 * Vanilla splits most "middle" climate slots by weirdness: negative weirdness gets the normal
 * biome, positive weirdness gets a variant (e.g. Old Growth Pine Taiga next to Old Growth Spruce
 * Taiga). The temperate, very humid slot has no variant, so it is Dark Forest on both sides. We
 * register the Whispering Woods as that missing variant.
 * <p>
 * Where the biome actually appears is decided afterwards by {@code MultiNoiseBiomeSourceMixin}: it
 * hands the <i>whole</i> Dark Forest slot to either Dark Forest or Whispering Woods, depending on a
 * large-scale region noise (see {@code WhisperingWoodsRegions}). No other biome loses any area, and the
 * terrain (hills, rivers, coasts) is unchanged.
 * <p>
 * Fabric API has no Overworld biome API, and TerraBlender would be a new dependency, so these two
 * small injections are the least invasive option.
 */
@Mixin(OverworldBiomeBuilder.class)
abstract class OverworldBiomeBuilderMixin {
	@Inject(method = "pickMiddleBiome", at = @At("RETURN"), cancellable = true)
	private void valor$addWhisperingWoods(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness,
			CallbackInfoReturnable<ResourceKey<Biome>> cir) {
		// Same condition vanilla uses to pick a variant (see pickMiddleBiome).
		if (cir.getReturnValue() == Biomes.DARK_FOREST && weirdness.max() >= 0L) {
			cir.setReturnValue(ValorBiomes.WHISPERING_WOODS);
		}
	}
}
