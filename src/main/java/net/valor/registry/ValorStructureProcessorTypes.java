package net.valor.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.valor.Valor;
import net.valor.world.level.levelgen.structure.templatesystem.TemplateTreeProcessor;

/**
 * Structure processor types added by Valor. Counterpart to vanilla's {@link StructureProcessorType} constants.
 */
public final class ValorStructureProcessorTypes {

	public static final StructureProcessorType<TemplateTreeProcessor> TEMPLATE_TREE = register("template_tree",
			() -> TemplateTreeProcessor.CODEC);

	private ValorStructureProcessorTypes() {}

	private static <P extends StructureProcessor> StructureProcessorType<P> register(String name, StructureProcessorType<P> type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR, Valor.id(name), type);
	}

	/** Forces class loading so all static fields get registered. */
	public static void init() {}
}
