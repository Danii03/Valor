package net.valor.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import net.valor.registry.ValorItems;

/**
 * Block states, block models and item models. Prefer vanilla's generator helpers so models
 * follow the same structure as vanilla ones.
 * <p>
 * Examples:
 * <pre>{@code
 * blocks.createTrivialCube(ValorBlocks.RUBY_BLOCK);
 * items.generateFlatItem(ValorItems.RUBY, ModelTemplates.FLAT_ITEM);
 * }</pre>
 * Custom (e.g. Blockbench) models are placed by hand in {@code assets/valor/models/}
 * and are not generated here.
 */
public final class ValorModelProvider extends FabricModelProvider {

	public ValorModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blocks) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators items) {
		items.generateFlatItem(ValorItems.ECHO_FLUTE, ModelTemplates.FLAT_ITEM);
	}
}
