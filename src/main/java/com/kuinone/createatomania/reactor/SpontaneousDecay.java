package com.kuinone.createatomania.reactor;

import net.minecraft.util.RandomSource;

/**
 * The spontaneous, flux-independent half of the reactor physics.
 * <p>
 * These reactions happen on their own, with no incoming neutron required, and are what make
 * a freshly placed fuel block warm itself up:
 * <ul>
 *   <li>U-238 self-fission, emitting a fast neutron</li>
 *   <li>I-135 decays into Xe-135</li>
 *   <li>Xe-135 decays into high-level waste</li>
 *   <li>high-level waste decays into low-level waste, still radiating</li>
 * </ul>
 */
public final class SpontaneousDecay {

	private SpontaneousDecay() {}

	/**
	 * Applies one step of spontaneous reactions.
	 *
	 * @param composition the fuel before the step
	 * @param random      the random source
	 * @param config      the configuration
	 * @return the fuel after decay, plus the flux released by self-fission and waste decay
	 */
	public static StepResult apply(FuelComposition composition, RandomSource random,
		com.kuinone.createatomania.AtomaniaConfig config) {

		FuelComposition.Builder fuel = composition.toBuilder();
		FluxVector released = new FluxVector();

		selfFission(fuel, released, random, config);
		iodineToXenon(fuel, random, config);
		xenonToWaste(fuel, random, config);
		wasteDecay(fuel, released, random, config);

		return new StepResult(fuel.build(), released);
	}

	/**
	 * U-238 spontaneous fission. Real U-238 does this very rarely, so the rate is a gameplay
	 * value in the config. Each event releases one fast neutron, which is what allows a cold
	 * reactor to bootstrap itself without an external neutron source.
	 */
	private static void selfFission(FuelComposition.Builder fuel, FluxVector released,
		RandomSource random, com.kuinone.createatomania.AtomaniaConfig config) {

		double u238 = fuel.get(Nuclide.U238);
		if (u238 <= 0.0D) {
			return;
		}
		// Scale by 0.01 so that a full 100-unit block yields the configured chance directly.
		double expected = config.spontaneousFissionChance.get() * u238 * 0.01D;
		int events = Reactions.count(expected, random);
		if (events > 0) {
			for (int i = 0; i < events; i++) {
				released.add(Reactions.randomDirection(random), FluxType.FAST_NEUTRON, 1.0D);
			}
		}
	}

	/** I-135 -> Xe-135. Iodine is the precursor to the xenon poison. */
	private static void iodineToXenon(FuelComposition.Builder fuel, RandomSource random,
		com.kuinone.createatomania.AtomaniaConfig config) {

		double iodine = fuel.get(Nuclide.I135);
		if (iodine <= 0.0D) {
			return;
		}
		int decayed = Reactions.count(config.iodineDecayChance.get() * iodine, random);
		if (decayed > 0) {
			double amount = Math.min(decayed, iodine);
			fuel.add(Nuclide.I135, -amount);
			fuel.add(Nuclide.XE135, amount);
		}
	}

	/** Xe-135 -> high-level waste. */
	private static void xenonToWaste(FuelComposition.Builder fuel, RandomSource random,
		com.kuinone.createatomania.AtomaniaConfig config) {

		double xenon = fuel.get(Nuclide.XE135);
		if (xenon <= 0.0D) {
			return;
		}
		int decayed = Reactions.count(config.xenonDecayChance.get() * xenon, random);
		if (decayed > 0) {
			double amount = Math.min(decayed, xenon);
			fuel.add(Nuclide.XE135, -amount);
			fuel.add(Nuclide.HIGH_WASTE, amount);
		}
	}

	/** High-level waste -> low-level waste, still emitting beta and gamma radiation. */
	private static void wasteDecay(FuelComposition.Builder fuel, FluxVector released,
		RandomSource random, com.kuinone.createatomania.AtomaniaConfig config) {

		double highWaste = fuel.get(Nuclide.HIGH_WASTE);
		if (highWaste <= 0.0D) {
			return;
		}
		int decayed = Reactions.count(config.highWasteDecayChance.get() * highWaste, random);
		if (decayed <= 0) {
			return;
		}
		double amount = Math.min(decayed, highWaste);
		fuel.add(Nuclide.HIGH_WASTE, -amount);
		fuel.add(Nuclide.LOW_WASTE, amount);

		// Decaying fission products are the dominant long-lived radiation source.
		released.add(Reactions.randomDirection(random), FluxType.GAMMA, amount * 0.5D);
		released.add(Reactions.randomDirection(random), FluxType.BETA, amount * 0.5D);
	}

	/** The fuel after a spontaneous step, plus the flux it released. */
	public record StepResult(FuelComposition composition, FluxVector released) {}
}