package com.kuinone.createatomania.reactor;

import net.minecraft.util.RandomSource;

/**
 * Stateless helpers for resolving probabilistic reaction counts.
 * <p>
 * Flux values are continuous rather than whole neutrons, so a reaction with probability
 * p against a flux of f has p * f expected events. The integer part of that expectation
 * always happens; the fractional remainder is settled by a single roll. This keeps each
 * reaction linear in flux without looping over individual neutrons.
 */
public final class Reactions {

	private Reactions() {}

	/**
	 * Resolves an expected event count into an integer number of events.
	 *
	 * @param expected the expected number of events, may be fractional
	 * @param random   the random source
	 * @return the realised event count, never negative
	 */
	public static int count(double expected, RandomSource random) {
		if (!(expected > 0.0D) || Double.isNaN(expected)) {
			return 0;
		}
		double whole = Math.floor(expected);
		double fraction = expected - whole;
		int result = (int) Math.min(whole, Integer.MAX_VALUE);
		if (fraction > 0.0D && random.nextDouble() < fraction) {
			result++;
		}
		return result;
	}

	/**
	 * Multiplies a per-unit probability by a flux value to get an expected event count.
	 *
	 * @param probability per-unit-flux probability, clamped to [0,1]
	 * @param flux        the incoming flux
	 * @return the expected number of events
	 */
	public static double expected(double probability, double flux) {
		if (flux <= 0.0D || probability <= 0.0D) {
			return 0.0D;
		}
		return Math.min(1.0D, probability) * flux;
	}

	/** @return true with the given probability. */
	public static boolean chance(double probability, RandomSource random) {
		if (probability <= 0.0D) {
			return false;
		}
		if (probability >= 1.0D) {
			return true;
		}
		return random.nextDouble() < probability;
	}

	/** @return a uniformly random face. */
	public static net.minecraft.core.Direction randomDirection(RandomSource random) {
		net.minecraft.core.Direction[] faces = net.minecraft.core.Direction.values();
		return faces[random.nextInt(faces.length)];
	}
}