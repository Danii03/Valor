package net.valor.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.valor.platform.ClientHooks;
import net.valor.registry.ValorBlockEntityTypes;
import net.valor.registry.ValorItems;
import net.valor.world.level.block.entity.EchoFluteAltarBlockEntity;

/**
 * An old stone altar that holds the Echo Flute. With the flute on it, turquoise soul fire burns along its
 * rim, it gives off a little light and crackles softly; without it, it is plain stone.
 * <p>
 * Works like a lectern: right-click with the flute to lay it down, right-click with an empty hand to take it
 * back. The flute itself is kept in a {@link EchoFluteAltarBlockEntity}; {@link #HAS_FLUTE} only mirrors it
 * for the model, the light level and the sound. Breaking the altar drops the flute.
 */
public class EchoFluteAltarBlock extends BaseEntityBlock {
	public static final MapCodec<EchoFluteAltarBlock> CODEC = simpleCodec(EchoFluteAltarBlock::new);
	public static final BooleanProperty HAS_FLUTE = BooleanProperty.create("has_flute");
	/** Light level while the flute rests on the altar: lights up its surroundings a little, like a soul lantern at half strength. */
	public static final int LIT_LIGHT_LEVEL = 7;

	/** Base slab, pillar and top slab, matching the model. */
	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
			Block.box(3.0, 3.0, 3.0, 13.0, 12.0, 13.0),
			Block.box(0.0, 12.0, 0.0, 16.0, 15.0, 16.0));

	public EchoFluteAltarBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(HAS_FLUTE, false));
	}

	/** Light level per state, for the block properties. */
	public static int lightLevel(BlockState state) {
		return state.getValue(HAS_FLUTE) ? LIT_LIGHT_LEVEL : 0;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HAS_FLUTE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** {@link BaseEntityBlock} renders nothing by default; the altar is a normal block model. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** Laying the flute down. Any other item, or a second flute, goes on to its normal use. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(ValorItems.ECHO_FLUTE)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (state.getValue(HAS_FLUTE) || !(level.getBlockEntity(pos) instanceof EchoFluteAltarBlockEntity altar)) {
			// Holding a flute at a full altar: just play it.
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide()) {
			// The server decides; the client only swings the arm. consume() keeps the flute in creative mode, like a lectern's book.
			altar.setFlute(stack.copyWithCount(1));
			stack.consume(1, player);
			level.setBlock(pos, state.setValue(HAS_FLUTE, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
			level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.6F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
	}

	/** Taking the flute back, with an empty hand only. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!state.getValue(HAS_FLUTE) || !player.getMainHandItem().isEmpty()
				|| !(level.getBlockEntity(pos) instanceof EchoFluteAltarBlockEntity altar)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack flute = altar.removeFlute();
			if (!flute.isEmpty()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, flute);
			}
			level.setBlock(pos, state.setValue(HAS_FLUTE, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	/** Drops the flute whenever the altar goes away (mined, blown up, replaced), like a lectern's book. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof EchoFluteAltarBlockEntity altar) {
				ItemStack flute = altar.removeFlute();
				if (!flute.isEmpty()) {
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, flute);
				}
			}
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** A few soul fire flames rise from the rim now and then. Kept rare: the animated fire does most of the work. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(HAS_FLUTE) || random.nextInt(3) != 0) {
			return;
		}
		// A point on the ring of flames (2 px inside the edge).
		double along = 0.125 + random.nextDouble() * 0.75;
		double across = random.nextBoolean() ? 0.125 : 0.875;
		boolean alongX = random.nextBoolean();
		double x = pos.getX() + (alongX ? along : across);
		double z = pos.getZ() + (alongX ? across : along);
		level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, pos.getY() + 1.0, z, 0.0, 0.01 + random.nextDouble() * 0.01, 0.0);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EchoFluteAltarBlockEntity(pos, state);
	}

	/** Client only: tells the sound handler that this altar is still there and whether it burns. */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? createTickerHelper(type, ValorBlockEntityTypes.ECHO_FLUTE_ALTAR,
				(tickLevel, pos, tickState, altar) -> ClientHooks.get().tickEchoFluteAltar(tickLevel, pos, tickState.getValue(HAS_FLUTE))) : null;
	}
}
