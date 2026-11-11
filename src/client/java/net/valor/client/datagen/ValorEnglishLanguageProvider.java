package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.valor.registry.ValorItems;

/**
 * English translations ({@code en_us}). Every name must also be added to
 * {@link ValorGermanLanguageProvider}.
 * <p>
 * Example: {@code builder.add(ValorBlocks.RUBY_BLOCK, "Block of Ruby");}
 */
public final class ValorEnglishLanguageProvider extends FabricLanguageProvider {

	public ValorEnglishLanguageProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, "en_us", registries);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
		builder.add(ValorItems.ECHO_FLUTE, "Echo Flute");
		builder.add("screen.valor.echo_flute", "Echo Flute");
		builder.add("biome.valor.whispering_woods", "Whispering Woods");
	}
}
