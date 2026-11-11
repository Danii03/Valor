package net.valor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.valor.Valor;
import net.valor.world.item.EchoFluteNote;

/**
 * Server → nearby clients: the player with {@code entityId} played a note. Never sent to the
 * player who played it, since that client already spawned its own effects.
 */
public record ClientboundEchoFluteNotePayload(int entityId, InteractionHand hand, EchoFluteNote note)
		implements CustomPacketPayload {

	public static final Type<ClientboundEchoFluteNotePayload> TYPE = new Type<>(Valor.id("echo_flute_note"));
	public static final StreamCodec<ByteBuf, ClientboundEchoFluteNotePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientboundEchoFluteNotePayload::entityId,
			ValorStreamCodecs.INTERACTION_HAND, ClientboundEchoFluteNotePayload::hand,
			EchoFluteNote.STREAM_CODEC, ClientboundEchoFluteNotePayload::note,
			ClientboundEchoFluteNotePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
