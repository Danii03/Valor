package net.valor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.valor.Valor;
import net.valor.world.item.EchoFluteNote;

/** Client → server: the local player played a note on the Echo Flute held in {@code hand}. */
public record ServerboundPlayEchoFluteNotePayload(InteractionHand hand, EchoFluteNote note)
		implements CustomPacketPayload {

	public static final Type<ServerboundPlayEchoFluteNotePayload> TYPE = new Type<>(Valor.id("play_echo_flute_note"));
	public static final StreamCodec<ByteBuf, ServerboundPlayEchoFluteNotePayload> STREAM_CODEC = StreamCodec.composite(
			ValorStreamCodecs.INTERACTION_HAND, ServerboundPlayEchoFluteNotePayload::hand,
			EchoFluteNote.STREAM_CODEC, ServerboundPlayEchoFluteNotePayload::note,
			ServerboundPlayEchoFluteNotePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
