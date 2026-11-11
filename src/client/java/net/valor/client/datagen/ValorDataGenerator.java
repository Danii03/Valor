package net.valor.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

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
		pack.addProvider(ValorEnglishLanguageProvider::new);
		pack.addProvider(ValorGermanLanguageProvider::new);
	}
}
