package com.kuinone.createatomania.content.fuel;

import com.kuinone.createatomania.reactor.FuelComposition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block of nuclear fuel.
 * <p>
 * The block itself holds no state; everything lives in {@link FuelBlockEntity}. What this
 * class adds is the Create interop: it exposes the fuel's nuclide inventory to the engineer's
 * goggles, and it makes sure the block item you get back when breaking it carries the exact
 * composition that was inside.
 */
public class FuelBlock extends Block implements EntityBlock {

	public FuelBlock(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FuelBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
		BlockEntityType<T> type) {
		// The simulation itself is driven centrally by the reactor manager rather than by a
		// per-block ticker, so that a whole reactor is solved in one coherent pass instead of
		// block by block in arbitrary order.
		return null;
	}

	/**
	 * Preserves the fuel composition in the dropped item.
	 * <p>
	 * {@code getCloneItemStack} is what both pick-block and the drop path consult, so
	 * overriding it here covers breaking the block, middle-clicking it and any mod that
	 * clones the stack.
	 */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		ItemStack stack = new ItemStack(this);
		if (level.getBlockEntity(pos) instanceof FuelBlockEntity fuel) {
			AtomaniaDataComponents.writeComposition(stack, fuel.getComposition());
		}
		return stack;
	}

	/** Convenience used by the drop path to attach composition without a level read. */
	public static ItemStack stackWith(ItemStack stack, FuelComposition composition) {
		AtomaniaDataComponents.writeComposition(stack, composition);
		return stack;
	}
}