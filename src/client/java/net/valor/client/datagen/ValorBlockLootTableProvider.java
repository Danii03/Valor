package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;

/**
 * Block loot tables (what a block drops when broken).
 * <p>
 * Example: {@code dropSelf(ValorBlocks.RUBY_BLOCK);}
 */
public final class ValorBlockLootTableProvider extends FabricBlockLootTableProvider {

	public ValorBlockLootTableProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
	}
}
