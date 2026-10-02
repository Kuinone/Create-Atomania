package com.kuinone.createatomania.reactor;

import com.kuinone.createatomania.AtomaniaConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a given block affects flux crossing it.
 * <p>
 * This is the single place that decides what counts as a moderator and what counts as a
 * shield, so the physics stays in one spot rather than being spread across block classes:
 * <ul>
 *   <li>water moderates neutrons strongly and also absorbs some</li>
 *   <li>coal moderates them less well and absorbs very little</li>
 *   <li>fuel blocks are their own medium, and do not moderate</li>
 *   <li>a control rod absorbs heavily, which is how it throttles the reaction</li>
 *   <li>anything else blocks flux entirely, so ordinary walls act as shielding</li>
 * </ul>
 */
public final class ReactorMedium {

	private ReactorMedium() {}

	/**
	 * @return true if a reactor's flux can travel through this position. This is what the
	 *         flood fill in {@link ReactorScanner} uses to decide the reactor's shape.
	 */
	public static boolean conducts(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (isParticipant(level, pos)) {
			return true;
		}
		return isWater(state) || isCoal(state);
	}

	/** @return true if this position holds a block that takes part in the simulation. */
	public static boolean isParticipant(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof FluxHolder;
	}

	/** @return the probability that a neutron crossing this block is slowed one energy rank. */
	public static double moderationChance(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (isWater(state)) {
			return AtomaniaConfig.INSTANCE.waterModerationChance.get();
		}
		if (isCoal(state)) {
			return AtomaniaConfig.INSTANCE.coalModerationChance.get();
		}
		if (level.getBlockEntity(pos) instanceof FluxHolder holder) {
			return holder.moderationChance();
		}
		return 0.0D;
	}

	/** @return the fraction of neutrons absorbed when crossing this block, in [0,1]. */
	public static double absorptionFraction(Level level, BlockPos pos, AtomaniaConfig config) {
		if (level.getBlockEntity(pos) instanceof FluxHolder holder) {
			return holder.absorptionFraction();
		}
		// Still water absorbs a trickle of neutrons on its own.
		if (isWater(level.getBlockState(pos))) {
			return 0.02D;
		}
		return 0.0D;
	}

	/** @return true for any of the water blocks the mod treats as a moderator. */
	public static boolean isWater(BlockState state) {
		return state.is(Blocks.WATER) || state.is(Blocks.WATER_CAULDRON);
	}

	/** @return true for the coal blocks that act as a moderator. */
	public static boolean isCoal(BlockState state) {
		return state.is(Blocks.COAL_BLOCK);
	}
}