package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

/**
 * Writes Valor's worldgen data (configured features, placed features, biomes, structures, structure sets) to
 * {@code data/valor/worldgen/}. The contents are defined in {@code datagen/worldgen} and added to
 * the registries in {@link ValorDataGenerator#buildRegistry}.
 */
public final class ValorWorldgenProvider extends FabricDynamicRegistryProvider {

	public ValorWorldgenProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.BIOME));
		entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE));
		entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE_SET));
	}

	@Override
	public String getName() {
		return "Valor Worldgen";
	}
}
