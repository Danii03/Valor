package net.valor.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.valor.Valor;
import net.valor.world.level.block.entity.EchoFluteAltarBlockEntity;

/**
 * All block entity types added by Valor. Counterpart to vanilla's {@link BlockEntityType} constants.
 * Registered after {@link ValorBlocks}, whose blocks they belong to.
 */
public final class ValorBlockEntityTypes {

	public static final BlockEntityType<EchoFluteAltarBlockEntity> ECHO_FLUTE_ALTAR = register("echo_flute_altar",
			EchoFluteAltarBlockEntity::new, ValorBlocks.ECHO_FLUTE_ALTAR);

	private ValorBlockEntityTypes() {}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntitySupplier<T> factory,
			Block... blocks) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Valor.id(name), BlockEntityType.Builder.of(factory, blocks).build(null));
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
