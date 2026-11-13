package net.valor.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.valor.client.datagen.worldgen.ValorBiomeData;
import net.valor.client.datagen.worldgen.ValorConfiguredFeatures;
import net.valor.client.datagen.worldgen.ValorPlacedFeatures;
import net.valor.client.datagen.worldgen.ValorStructureData;

/**
 * Datagen entrypoint. Run with {@code ./gradlew runDatagen}; output goes to {@code src/main/generated}.
 * Never edit generated files by hand – change the providers and re-run datagen instead.
 */
public final class ValorDataGenerator implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();

		pack.addProvider(ValorModelProvider::new);
		pack.addProvider(ValorBlockLootTableProvider::new);
		pack.addProvider(ValorRecipeProvider::new);
		pack.addProvider(ValorBlockTagProvider::new);
		pack.addProvider(ValorItemTagProvider::new);
		pack.addProvider(ValorBiomeTagProvider::new);
		pack.addProvider(ValorWorldgenProvider::new);
		pack.addProvider(ValorEnglishLanguageProvider::new);
		pack.addProvider(ValorGermanLanguageProvider::new);
	}

	/** Worldgen content (dynamic registries), written out by {@link ValorWorldgenProvider}. */
	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.CONFIGURED_FEATURE, ValorConfiguredFeatures::bootstrap);
		registryBuilder.add(Registries.PLACED_FEATURE, ValorPlacedFeatures::bootstrap);
		registryBuilder.add(Registries.BIOME, ValorBiomeData::bootstrap);
		registryBuilder.add(Registries.STRUCTURE, ValorStructureData::bootstrapStructures);
		registryBuilder.add(Registries.STRUCTURE_SET, ValorStructureData::bootstrapStructureSets);
	}
}
