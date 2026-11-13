package net.valor.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.valor.Valor;
import net.valor.world.level.levelgen.structure.structures.RuinPiece;

/**
 * Structure piece types added by Valor. Counterpart to vanilla's {@link StructurePieceType} constants.
 * Piece types let saved chunks load their unfinished structure pieces again.
 */
public final class ValorStructurePieceTypes {

	public static final StructurePieceType RUIN = register("ruin", (StructurePieceType.StructureTemplateType) RuinPiece::new);

	private ValorStructurePieceTypes() {}

	private static StructurePieceType register(String name, StructurePieceType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Valor.id(name), type);
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
