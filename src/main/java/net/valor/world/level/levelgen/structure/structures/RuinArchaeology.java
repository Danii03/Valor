package net.valor.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The dig site around a ruin: irregular patches of rubble-strewn ground and a few blocks of suspicious
 * gravel that can be brushed like any vanilla dig site, with vanilla archaeology loot.
 *
 * @param patches      number of ground patches around the ruin
 * @param patchRadius  radius of each patch; edges are frayed, so patches are never round
 * @param ground       blocks of a patch (grass blocks may be part of the mix, so grass shows between rubble)
 * @param suspicious   number of suspicious gravel blocks, placed only where the ground is suitable
 * @param commonLoot   loot table of most suspicious gravel
 * @param rareLoot     loot table of the occasional rare find
 * @param rareChance   chance per suspicious gravel block to use {@code rareLoot}
 * @param buriedChance chance per suspicious gravel block to lie one block under the surface
 */
public record RuinArchaeology(IntProvider patches, FloatProvider patchRadius, BlockStateProvider ground, IntProvider suspicious,
		ResourceKey<LootTable> commonLoot, ResourceKey<LootTable> rareLoot, float rareChance, float buriedChance) {

	public static final Codec<RuinArchaeology> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			IntProvider.codec(0, 12).fieldOf("patches").forGetter(RuinArchaeology::patches),
			FloatProvider.codec(0.5F, 4.0F).fieldOf("patch_radius").forGetter(RuinArchaeology::patchRadius),
			BlockStateProvider.CODEC.fieldOf("ground").forGetter(RuinArchaeology::ground),
			IntProvider.codec(0, 12).fieldOf("suspicious").forGetter(RuinArchaeology::suspicious),
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("common_loot").forGetter(RuinArchaeology::commonLoot),
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("rare_loot").forGetter(RuinArchaeology::rareLoot),
			Codec.floatRange(0.0F, 1.0F).fieldOf("rare_chance").forGetter(RuinArchaeology::rareChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("buried_chance").forGetter(RuinArchaeology::buriedChance)
	).apply(instance, RuinArchaeology::new));
}
