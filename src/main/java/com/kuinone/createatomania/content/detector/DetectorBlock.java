package com.kuinone.createatomania.content.detector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A counter block that emits a redstone signal proportional to what it measures.
 * <p>
 * The output is derived from a logarithmic scale, so the signal rises quickly from zero and
 * then saturates, which is the behaviour a player wants when wiring a reactor alarm: a clear
 * distinction between "quiet", "running" and "far too hot".
 */
public class DetectorBlock extends Block implements EntityBlock {

	private final DetectorKind kind;

	public DetectorBlock(Properties properties, DetectorKind kind) {
		super(properties);
		this.kind = kind;
	}

	public DetectorKind getKind() {
		return kind;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DetectorBlockEntity(pos, state, kind);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
		BlockEntityType<T> type) {
		// Sampling is driven by the reactor simulation sweep, not a per-block ticker, so
		// that the reading is always consistent with the step that produced it.
		return null;
	}

	/** Feeds the reading out as a redstone signal. */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		if (level.getBlockEntity(pos) instanceof DetectorBlockEntity detector) {
			return detector.getRedstoneStrength();
		}
		return 0;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** Also drives comparators, so a player can read the value as an analog level. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getSignal(state, level, pos, Direction.UP);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
		boolean movedByPiston) {

		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			// Detectors are tracked as reactor seeds too, so a counter standing on its own
			// beside a reactor still refreshes even if no fuel block is scanned nearby.
			com.kuinone.createatomania.reactor.ReactorTracker.track(serverLevel, pos);
		}
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
		boolean movedByPiston) {

		if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			com.kuinone.createatomania.reactor.ReactorTracker.untrack(serverLevel, pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}
}