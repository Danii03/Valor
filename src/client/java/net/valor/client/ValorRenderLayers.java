package net.valor.client;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.valor.registry.ValorBlocks;

/**
 * Assigns non-solid render layers to blocks that need them.
 * <ul>
 *     <li>{@code cutout} – fully transparent pixels (flowers, saplings, doors, trapdoors)</li>
 *     <li>{@code cutoutMipped} – like cutout, but for leaves-like blocks</li>
 *     <li>{@code translucent} – partially transparent pixels (glass, ice, slime)</li>
 * </ul>
 * Example:
 * <pre>{@code
 * BlockRenderLayerMap.INSTANCE.putBlock(ValorBlocks.RUBY_DOOR, RenderType.cutout());
 * }</pre>
 */
public final class ValorRenderLayers {

	private ValorRenderLayers() {}

	public static void init() {
		// ---- Render layers go here -------------------------------------------------------

		// The soul fire on a lit altar has transparent pixels, like vanilla's soul fire.
		BlockRenderLayerMap.INSTANCE.putBlock(ValorBlocks.ECHO_FLUTE_ALTAR, RenderType.cutout());
	}
}
