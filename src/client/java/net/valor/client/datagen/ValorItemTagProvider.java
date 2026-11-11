package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;

/**
 * Item tag contents – both Valor's own tags and additions to vanilla tags
 * (e.g. {@code ItemTags.BEACON_PAYMENT_ITEMS}, {@code ItemTags.TRIM_MATERIALS}).
 */
public final class ValorItemTagProvider extends FabricTagProvider.ItemTagProvider {

	public ValorItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
	}
}
