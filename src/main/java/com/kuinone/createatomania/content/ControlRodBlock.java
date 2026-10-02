package com.kuinone.createatomania.content;

import com.kuinone.createatomania.AtomaniaBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A control rod.
 * <p>
 * This is the player's brake on the chain reaction. It absorbs a fraction of every neutron
 * crossing it, and how much depends on how far the rod is inserted, which in turn is driven
 * by the redstone signal it receives: no signal means fully withdrawn, full power means fully
 * inserted. Rods slide in and out over a few ticks rather than snapping, so a reactor
 * responds to redstone the way a real one responds to rod drives.
 */
public class ControlRodBlock extends Block implements EntityBlock {

	/** Redstone signal strength, mirrored into the block state for rendering and goggles. */
	public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);

	public ControlRodBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWER);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ControlRodBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
		BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return (tickLevel, pos, tickState, blockEntity) -> {
			if (blockEntity instanceof ControlRodBlockEntity rod) {
				rod.tickInsertion();
			}
		};
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
		BlockPos neighborPos, boolean movedByPiston) {

		super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
		if (level.isClientSide()) {
			return;
		}
		int signal = level.getBestNeighborSignal(pos);
		tickSignal(level, pos, state, signal);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
		boolean movedByPiston) {

		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide()) {
			tickSignal(level, pos, state, level.getBestNeighborSignal(pos));
		}
	}

	/** Pushes a new signal strength into the block entity and the block state. */
	private void tickSignal(Level level, BlockPos pos, BlockState state, int signal) {
		if (level.getBlockEntity(pos) instanceof ControlRodBlockEntity rod) {
			rod.setRedstoneSignal(signal);
		}
		if (state.getValue(POWER) != signal) {
			level.setBlock(pos, state.setValue(POWER, signal), Block.UPDATE_CLIENTS);
		}
	}
}