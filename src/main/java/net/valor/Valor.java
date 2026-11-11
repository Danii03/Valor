package net.valor;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.valor.registry.ValorBlocks;
import net.valor.registry.ValorCreativeTabs;
import net.valor.network.ValorNetworking;
import net.valor.registry.ValorFeatures;
import net.valor.registry.ValorStructureProcessorTypes;
import net.valor.registry.ValorItems;
import net.valor.registry.ValorParticleTypes;
import net.valor.registry.ValorSounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint of Valor. Runs on both the client and the dedicated server.
 * <p>
 * Only bootstraps the registries here. Gameplay logic lives in its own classes.
 */
public final class Valor implements ModInitializer {
	public static final String MOD_ID = "valor";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Order matters: anything referenced by a later registry has to be registered first.
		ValorSounds.init();
		ValorBlocks.init();
		ValorItems.init();
		ValorParticleTypes.init();
		ValorStructureProcessorTypes.init();
		ValorFeatures.init();
		ValorCreativeTabs.init();
		ValorNetworking.init();

		LOGGER.info("Valor initialized");
	}

	/** Builds a {@code valor:<path>} identifier. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
