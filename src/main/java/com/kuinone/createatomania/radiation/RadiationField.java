package com.kuinone.createatomania.radiation;

import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.content.fuel.FuelBlockEntity;
import com.kuinone.createatomania.reactor.FluxHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Works out the radiation dose rate at a position.
 * <p>
 * Dose comes from two places: radiation that escapes a reactor block into the surrounding
 * air, and the ambient activity of dropped radioactive items. Both fall off with distance.
 * <p>
 * The attenuation is what makes shielding work. A reactor buried in concrete or with a water
 * jacket around it doses a player far less than an exposed one, because the flux has to cross
 * those blocks to get out.
 */
public final class RadiationField {

	private RadiationField() {}

	/** Maximum distance a block's radiation is felt, in blocks. */
	private static final int MAX_RANGE = 6;

	/**
	 * Sums the dose rate in sieverts per second at a position.
	 *
	 * @param level  the level
	 * @param pos    the position to sample
	 * @param config the configuration
	 * @return the dose rate in sieverts per second, never negative
	 */
	public static double doseAt(Level level, BlockPos pos, AtomaniaConfig config) {
		double dose = 0.0D;

		// Radiation escaping nearby reactor blocks.
		for (BlockPos candidate : BlockPos.betweenClosed(
			pos.offset(-MAX_RANGE, -MAX_RANGE, -MAX_RANGE),
			pos.offset(MAX_RANGE, MAX_RANGE, MAX_RANGE))) {

			if (!level.isLoaded(candidate)) {
				continue;
			}
			if (!(level.getBlockEntity(candidate) instanceof FluxHolder holder)) {
				continue;
			}

			double emitted = emittedRadiation(holder);
			if (emitted <= 0.0D) {
				continue;
			}

			double distance = Math.sqrt(candidate.distSqr(pos));
			if (distance < 0.5D) {
				distance = 0.5D;
			}

			// Inverse-square falloff, further reduced by everything between here and there.
			double transmission = shieldingBetween(level, candidate, pos, config);
			dose += emitted * transmission / (distance * distance);
		}

		// Dose from radioactive items lying on the ground.
		double groundActivity = RadiationSystem.groundActivity(level, pos);
		if (groundActivity > 0.0D) {
			dose += groundActivity * config.dosePerHeldItem.get();
		}

		return Math.max(0.0D, dose);
	}

	/** @return the radiation flux a holder emitted during the last step. */
	private static double emittedRadiation(FluxHolder holder) {
		if (holder instanceof FuelBlockEntity fuel) {
			return fuel.getLastRadiation();
		}
		return 0.0D;
	}

	/**
	 * Estimates how much of the radiation survives the trip between two positions.
	 * <p>
	 * Each solid block on the path multiplies the flux by a shielding factor, so a single
	 * layer of stone only halves the dose but a thick wall reduces it to nothing. This is a
	 * straight-line sample rather than a real ray cast; it is cheap and behaves the way a
	 * player expects.
	 */
	private static double shieldingBetween(Level level, BlockPos from, BlockPos to, AtomaniaConfig config) {
		double transmission = 1.0D;
		int steps = (int) Math.ceil(Math.sqrt(from.distSqr(to)));
		if (steps <= 1) {
			return transmission;
		}

		for (int i = 1; i < steps; i++) {
			double t = (double) i / steps;
			BlockPos sample = BlockPos.containing(
				from.getX() + 0.5D + (to.getX() - from.getX()) * t,
				from.getY() + 0.5D + (to.getY() - from.getY()) * t,
				from.getZ() + 0.5D + (to.getZ() - from.getZ()) * t);

			if (sample.equals(from) || sample.equals(to)) {
				continue;
			}
			transmission *= blockTransmission(level, sample, config);
			if (transmission < 1.0E-4D) {
				return 0.0D;
			}
		}
		return transmission;
	}

	/** @return the fraction of radiation that survives passing through one block. */
	private static double blockTransmission(Level level, BlockPos pos, AtomaniaConfig config) {
		var state = level.getBlockState(pos);

		// Air and other non-solid blocks barely interact.
		if (state.isAir()) {
			return 1.0D;
		}
		// Water is a good shield as well as a moderator.
		if (state.is(net.minecraft.world.level.block.Blocks.WATER)) {
			return 0.35D;
		}
		// Everything solid blocks most of it.
		return 0.25D;
	}
}