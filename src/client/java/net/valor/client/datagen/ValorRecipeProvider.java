package net.valor.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

/**
 * Crafting, smelting, stonecutting and smithing recipes. Use vanilla's helpers
 * (e.g. {@code nineBlockStorageRecipes}, {@code stairBuilder}, {@code oreSmelting}) where possible,
 * so recipes and recipe-book unlocks behave like vanilla ones.
 */
public final class ValorRecipeProvider extends FabricRecipeProvider {

	public ValorRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void buildRecipes(RecipeOutput output) {
	}
}
