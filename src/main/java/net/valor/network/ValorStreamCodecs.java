package net.valor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;

/** Shared stream codecs for vanilla types that don't ship their own. */
public final class ValorStreamCodecs {

	public static final StreamCodec<ByteBuf, InteractionHand> INTERACTION_HAND = ByteBufCodecs.idMapper(
			ByIdMap.continuous(InteractionHand::ordinal, InteractionHand.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
			InteractionHand::ordinal);

	private ValorStreamCodecs() {}
}
