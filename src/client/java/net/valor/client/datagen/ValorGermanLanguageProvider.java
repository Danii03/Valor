package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.valor.registry.ValorItems;

/**
 * German translations ({@code de_de}). Follow the wording of the official German
 * Minecraft translation (e.g. "Rubinblock", not "Block aus Rubin").
 * <p>
 * Example: {@code builder.add(ValorBlocks.RUBY_BLOCK, "Rubinblock");}
 */
public final class ValorGermanLanguageProvider extends FabricLanguageProvider {

	public ValorGermanLanguageProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, "de_de", registries);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
		builder.add(ValorItems.ECHO_FLUTE, "Echoflöte");
		builder.add("screen.valor.echo_flute", "Echoflöte");
		builder.add("biome.valor.whispering_woods", "Flüsterwald");
	}
}
