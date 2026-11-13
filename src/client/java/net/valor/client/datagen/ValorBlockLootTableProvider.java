package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Blocks;
import net.valor.registry.ValorBlocks;

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
		// Like spruce leaves: itself with shears/Silk Touch, otherwise spruce saplings and sticks.
		add(ValorBlocks.WHISPERING_LEAVES, block -> createLeavesDrops(block, Blocks.SPRUCE_SAPLING, NORMAL_LEAVES_SAPLING_CHANCES));
		// The flute on it drops separately (EchoFluteAltarBlock#onRemove).
		dropSelf(ValorBlocks.ECHO_FLUTE_ALTAR);
	}
}
