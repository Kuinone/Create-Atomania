package com.kuinone.createatomania.content;

import com.kuinone.createatomania.AtomaniaBlockEntities;
import com.kuinone.createatomania.reactor.FluxHolder;
import com.kuinone.createatomania.reactor.FluxVector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A control rod's state.
 * <p>
 * A rod absorbs a fraction of every neutron that crosses it. How much depends on how far it
 * is inserted, which is driven by the redstone signal it receives: unpowered means fully
 * withdrawn and the reactor runs, full power means fully inserted and the reaction is
 * damped. That gives a player a lever on the chain reaction without touching the fuel.
 */
public class ControlRodBlockEntity extends BlockEntity implements FluxHolder {

	/** Insertion depth from 0 (withdrawn) to 1 (fully inserted). */
	private double insertion;

	/** Insertion the rod is easing toward, so rods move rather than snap. */
	private double targetInsertion;

	private final FluxVector incomingFlux = new FluxVector();
	private final FluxVector outgoingFlux = new FluxVector();

	public ControlRodBlockEntity(BlockPos pos, BlockState state) {
		super(AtomaniaBlockEntities.CONTROL_ROD.get(), pos, state);
	}

	@Override
	public FluxVector getIncomingFlux() {
		return incomingFlux;
	}

	@Override
	public FluxVector getOutgoingFlux() {
		return outgoingFlux;
	}

	public double getInsertion() {
		return insertion;
	}

	/** Sets the target insertion from a redstone signal strength in [0,15]. */
	public void setRedstoneSignal(int signal) {
		this.targetInsertion = Mth.clamp(signal / 15.0D, 0.0D, 1.0D);
	}

	public double getTargetInsertion() {
		return targetInsertion;
	}

	/** Eases the rod toward its target. Called once per simulation step. */
	public void tickInsertion() {
		if (insertion == targetInsertion) {
			return;
		}
		double speed = 0.05D;
		if (insertion < targetInsertion) {
			insertion = Math.min(targetInsertion, insertion + speed);
		} else {
			insertion = Math.max(targetInsertion, insertion - speed);
		}
		setChanged();
	}

	/**
	 * A control rod has no fuel, so it never reacts or emits; it only absorbs.
	 * <p>
	 * The value is scaled by insertion depth, so a withdrawn rod is simply not there.
	 */
	@Override
	public double absorptionFraction() {
		return com.kuinone.createatomania.AtomaniaConfig.INSTANCE.controlRodAbsorption.get() * insertion;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putDouble("Insertion", insertion);
		tag.putDouble("TargetInsertion", targetInsertion);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		insertion = tag.getDouble("Insertion");
		targetInsertion = tag.getDouble("TargetInsertion");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}