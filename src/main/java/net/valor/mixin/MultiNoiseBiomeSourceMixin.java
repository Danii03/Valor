package net.valor.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.valor.world.level.biome.ValorBiomes;
import net.valor.world.level.biome.WhisperingWoodsRegions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the Whispering Woods rare but large (see {@link WhisperingWoodsRegions}):
 * <ul>
 *     <li>inside the Dark Forest climate slot, the large-scale region noise picks Whispering Woods or Dark
 *     Forest for the whole slot, instead of splitting it by weirdness (which gives thin, striped patches);</li>
 *     <li>inside a region, a neighboring temperate forest becomes Whispering Woods where the region's slightly
 *     shifted climate falls into that slot, so the forest grows along the climate into one large area.</li>
 * </ul>
 * Only touches results that are Dark Forest, Whispering Woods or one of a few temperate forests; every other
 * biome and the terrain (which does not depend on temperature or humidity) stay vanilla. Both biomes are part
 * of the parameter list (see {@code OverworldBiomeBuilderMixin}), so features, structures and {@code /locate}
 * know about them.
 */
@Mixin(MultiNoiseBiomeSource.class)
abstract class MultiNoiseBiomeSourceMixin {
	/** {@code [darkForest, whisperingWoods]}, resolved once; an empty array if one of them is missing. */
	@Unique
	private volatile Holder<Biome>[] valor$forestBiomes;

	@Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
			at = @At("RETURN"), cancellable = true)
	private void valor$pickWhisperingWoodsRegion(int x, int y, int z, Climate.Sampler sampler,
			CallbackInfoReturnable<Holder<Biome>> cir) {
		Holder<Biome>[] forests = this.valor$forestBiomes();
		if (forests.length == 0) {
			return;
		}
		Holder<Biome> biome = cir.getReturnValue();
		if (biome == forests[0] || biome == forests[1]) {
			cir.setReturnValue(WhisperingWoodsRegions.contains(x, z) ? forests[1] : forests[0]);
			return;
		}
		float growth = WhisperingWoodsRegions.growth(x, z);
		if (growth > 0.0F && WhisperingWoodsRegions.canGrowInto(biome)) {
			// Same parameter search as vanilla, with the region's climate; never recurses into this injection.
			Holder<Biome> grown = ((MultiNoiseBiomeSource) (Object) this).getNoiseBiome(WhisperingWoodsRegions.shift(sampler.sample(x, y, z), growth));
			if (grown == forests[0] || grown == forests[1]) {
				cir.setReturnValue(forests[1]);
			}
		}
	}

	@Unique
	@SuppressWarnings("unchecked")
	private Holder<Biome>[] valor$forestBiomes() {
		Holder<Biome>[] forests = this.valor$forestBiomes;
		if (forests == null) {
			Holder<Biome> darkForest = null;
			Holder<Biome> whisperingWoods = null;
			for (Holder<Biome> biome : ((BiomeSource) (Object) this).possibleBiomes()) {
				if (biome.is(Biomes.DARK_FOREST)) {
					darkForest = biome;
				} else if (biome.is(ValorBiomes.WHISPERING_WOODS)) {
					whisperingWoods = biome;
				}
			}
			forests = darkForest != null && whisperingWoods != null ? new Holder[] {darkForest, whisperingWoods} : new Holder[0];
			this.valor$forestBiomes = forests;
		}
		return forests;
	}
}
