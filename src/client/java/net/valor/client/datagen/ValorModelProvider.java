package net.valor.client.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.valor.registry.ValorBlocks;
import net.valor.registry.ValorItems;
import net.valor.world.level.block.EchoFluteAltarBlock;

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
		// Vanilla's grey spruce leaves texture with the leaves template (tint index 0); the color is a tint.
		blocks.createTrivialBlock(ValorBlocks.WHISPERING_LEAVES, TexturedModel.createDefault(
				block -> TextureMapping.cube(TextureMapping.getBlockTexture(Blocks.SPRUCE_LEAVES)), ModelTemplates.LEAVES));

		// Hand-made Blockbench models: the bare altar, and the lit one with the ring of soul fire (only while it holds the flute).
		ResourceLocation altar = ModelLocationUtils.getModelLocation(ValorBlocks.ECHO_FLUTE_ALTAR);
		ResourceLocation litAltar = ModelLocationUtils.getModelLocation(ValorBlocks.ECHO_FLUTE_ALTAR, "_lit");
		blocks.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ValorBlocks.ECHO_FLUTE_ALTAR)
				.with(BlockModelGenerators.createBooleanModelDispatch(EchoFluteAltarBlock.HAS_FLUTE, litAltar, altar)));
		blocks.delegateItemModel(ValorBlocks.ECHO_FLUTE_ALTAR, altar);
	}

	@Override
	public void generateItemModels(ItemModelGenerators items) {
		items.generateFlatItem(ValorItems.ECHO_FLUTE, ModelTemplates.FLAT_ITEM);
	}
}
