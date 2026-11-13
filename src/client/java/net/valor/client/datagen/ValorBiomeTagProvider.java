package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.valor.tag.ValorBiomeTags;
import net.valor.world.level.biome.ValorBiomes;

/**
 * Biome tag contents. Vanilla tags decide which structures generate in a biome and how it is
 * treated by game logic, so a new biome must join the same tags as its vanilla relatives.
 */
public final class ValorBiomeTagProvider extends FabricTagProvider<Biome> {

	public ValorBiomeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.BIOME, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Same structure set as vanilla's old-growth taigas, minus villages and outposts.
		getOrCreateTagBuilder(BiomeTags.IS_OVERWORLD).add(ValorBiomes.WHISPERING_WOODS);
		getOrCreateTagBuilder(BiomeTags.IS_FOREST).add(ValorBiomes.WHISPERING_WOODS);
		getOrCreateTagBuilder(BiomeTags.HAS_MINESHAFT).add(ValorBiomes.WHISPERING_WOODS);
		getOrCreateTagBuilder(BiomeTags.HAS_RUINED_PORTAL_STANDARD).add(ValorBiomes.WHISPERING_WOODS);
		getOrCreateTagBuilder(BiomeTags.HAS_TRAIL_RUINS).add(ValorBiomes.WHISPERING_WOODS);
		getOrCreateTagBuilder(BiomeTags.STRONGHOLD_BIASED_TO).add(ValorBiomes.WHISPERING_WOODS);

		// Valor's own structures.
		getOrCreateTagBuilder(ValorBiomeTags.HAS_WHISPERING_WOODS_RUINS).add(ValorBiomes.WHISPERING_WOODS);
	}
}
