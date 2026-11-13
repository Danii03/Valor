package net.valor.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valor.registry.ValorBlockEntityTypes;

/**
 * Holds the Echo Flute lying on an {@link net.valor.world.level.block.EchoFluteAltarBlock}. Saved with the
 * chunk and sent to every client that sees the altar, so all players see the same flute.
 * <p>
 * The stack is only ever changed on the server, in one tick, by {@link #setFlute} and {@link #removeFlute};
 * whoever takes it out owns the only copy.
 */
public class EchoFluteAltarBlockEntity extends BlockEntity {
	private static final String FLUTE_TAG = "flute";

	private ItemStack flute = ItemStack.EMPTY;

	public EchoFluteAltarBlockEntity(BlockPos pos, BlockState state) {
		super(ValorBlockEntityTypes.ECHO_FLUTE_ALTAR, pos, state);
	}

	public ItemStack getFlute() {
		return this.flute;
	}

	public boolean hasFlute() {
		return !this.flute.isEmpty();
	}

	public void setFlute(ItemStack flute) {
		this.flute = flute;
		this.changed();
	}

	/** Takes the flute out; the altar is empty afterwards. */
	public ItemStack removeFlute() {
		ItemStack removed = this.flute;
		this.flute = ItemStack.EMPTY;
		this.changed();
		return removed;
	}

	private void changed() {
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (!this.flute.isEmpty()) {
			tag.put(FLUTE_TAG, this.flute.save(registries));
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		this.flute = tag.contains(FLUTE_TAG) ? ItemStack.parseOptional(registries, tag.getCompound(FLUTE_TAG)) : ItemStack.EMPTY;
	}

	/**
	 * Chunk data for clients: the flute, so it renders for everyone. The flute entry is always written, empty
	 * or not: the client skips block entity packets with an empty tag, so an empty altar would otherwise keep
	 * showing the flute that was just taken (vanilla's campfire writes its empty item list for the same reason).
	 */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = this.saveCustomOnly(registries);
		if (!tag.contains(FLUTE_TAG)) {
			tag.put(FLUTE_TAG, new CompoundTag());
		}
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
