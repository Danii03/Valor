package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;

/**
 * Block tag contents – both Valor's own tags and additions to vanilla tags.
 * Adding blocks to vanilla tags (mineable/pickaxe, needs_iron_tool, logs, ...) is what makes
 * them interact with the rest of the game correctly.
 * <p>
 * Example:
 * <pre>{@code
 * getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(ValorBlocks.RUBY_BLOCK);
 * getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).add(ValorBlocks.RUBY_BLOCK);
 * }</pre>
 */
public final class ValorBlockTagProvider extends FabricTagProvider.BlockTagProvider {

	public ValorBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
	}
}
