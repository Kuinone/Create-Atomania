package com.kuinone.createatomania.reactor;

import com.kuinone.createatomania.AtomaniaConfig;

import net.minecraft.util.RandomSource;

/**
 * The neutron-driven half of the reactor physics.
 * <p>
 * Given the flux arriving on each face of a fuel sample, this applies the four reactions
 * that make a reactor run:
 * <ul>
 *   <li>U-235 hit by a slow neutron fissions into fast neutrons, a little I-135, a little
 *       high-level waste and some alpha/beta/gamma radiation</li>
 *   <li>U-238 hit by an intermediate neutron captures it and breeds Pu-239, plus high-level
 *       waste and radiation</li>
 *   <li>Pu-239 hit by a slow neutron fissions into ultrafast neutrons, high-level waste, a
 *       little I-135 and radiation</li>
 *   <li>Xe-135 hit by a slow neutron burns out into high-level waste</li>
 * </ul>
 * Every reaction consumes the neutron that triggered it.
 */
public final class NeutronReactions {

	private NeutronReactions() {}

	/**
	 * Applies all neutron-driven reactions for one step.
	 *
	 * @param composition the fuel before the step
	 * @param incoming    flux arriving per face
	 * @param random      the random source
	 * @param config      the configuration
	 * @return the fuel after the step, the flux released, and the flux absorbed
	 */
	public static ReactionResult apply(FuelComposition composition, FluxVector incoming,
		RandomSource random, AtomaniaConfig config) {

		FuelComposition.Builder fuel = composition.toBuilder();
		FluxVector released = new FluxVector();
		double absorbedSlow = 0.0D;
		double absorbedIntermediate = 0.0D;

		double slowFlux = incoming.total(FluxType.SLOW_NEUTRON);
		double intermediateFlux = incoming.total(FluxType.INTERMEDIATE_NEUTRON);

		// --- U-235 fission, driven by slow neutrons -------------------------------
		double u235 = fuel.get(Nuclide.U235);
		if (u235 > 0.0D && slowFlux > 0.0D) {
			double expected = Reactions.expected(config.u235FissionChance.get(), slowFlux);
			int events = Reactions.count(Math.min(expected, u235), random);
			if (events > 0) {
				double burned = Math.min(events, u235);
				fuel.add(Nuclide.U235, -burned);
				absorbedSlow += burned / Math.max(1.0E-6D, config.u235FissionChance.get());

				// Fast neutrons: the yield per fission, so a chain reaction is possible when
				// the yield exceeds the losses to leakage and parasitic capture.
				double neutrons = burned * config.u235NeutronYield.get();
				emit(released, FluxType.FAST_NEUTRON, neutrons, random);

				// A small fraction of fissions land on the iodine mass-135 chain.
				double iodine = burned * config.u235IodineYield.get();
				fuel.add(Nuclide.I135, iodine);

				// The rest of the fission products are high-level waste.
				fuel.add(Nuclide.HIGH_WASTE, Math.max(0.0D, burned - iodine));

				// Fresh fission products are intensely radioactive.
				emit(released, FluxType.GAMMA, burned * 1.2D, random);
				emit(released, FluxType.BETA, burned * 0.8D, random);
				emit(released, FluxType.ALPHA, burned * 0.25D, random);
			}
		}

		// --- U-238 breeding, driven by intermediate neutrons ----------------------
		double u238 = fuel.get(Nuclide.U238);
		if (u238 > 0.0D && intermediateFlux > 0.0D) {
			double expected = Reactions.expected(config.u238BreedChance.get(), intermediateFlux);
			int events = Reactions.count(Math.min(expected, u238), random);
			if (events > 0) {
				double captured = Math.min(events, u238);
				fuel.add(Nuclide.U238, -captured);
				absorbedIntermediate += captured / Math.max(1.0E-6D, config.u238BreedChance.get());

				double plutonium = captured * config.u238PlutoniumYield.get();
				fuel.add(Nuclide.PU239, plutonium);
				fuel.add(Nuclide.HIGH_WASTE, Math.max(0.0D, captured - plutonium));

				// Neutron capture releases prompt gamma rays.
				emit(released, FluxType.GAMMA, captured * 0.9D, random);
			}
		}

		// --- Pu-239 fission, driven by slow neutrons ------------------------------
		double pu239 = fuel.get(Nuclide.PU239);
		if (pu239 > 0.0D && slowFlux > 0.0D) {
			double expected = Reactions.expected(config.pu239FissionChance.get(), slowFlux);
			int events = Reactions.count(Math.min(expected, pu239), random);
			if (events > 0) {
				double burned = Math.min(events, pu239);
				fuel.add(Nuclide.PU239, -burned);
				absorbedSlow += burned / Math.max(1.0E-6D, config.pu239FissionChance.get());

				double neutrons = burned * config.pu239NeutronYield.get();
				emit(released, FluxType.ULTRAFAST_NEUTRON, neutrons, random);

				double iodine = burned * config.pu239IodineYield.get();
				fuel.add(Nuclide.I135, iodine);
				fuel.add(Nuclide.HIGH_WASTE, Math.max(0.0D, burned - iodine));

				emit(released, FluxType.GAMMA, burned * 1.5D, random);
				emit(released, FluxType.BETA, burned * 1.0D, random);
				emit(released, FluxType.ALPHA, burned * 0.3D, random);
			}
		}

		// --- Xe-135 burnout, driven by slow neutrons ------------------------------
		double xenon = fuel.get(Nuclide.XE135);
		if (xenon > 0.0D && slowFlux > 0.0D) {
			double expected = Reactions.expected(config.xenonBurnoutChance.get(), slowFlux);
			int events = Reactions.count(Math.min(expected, xenon), random);
			if (events > 0) {
				double burned = Math.min(events, xenon);
				fuel.add(Nuclide.XE135, -burned);
				fuel.add(Nuclide.HIGH_WASTE, burned);
				// Xenon is a pure neutron poison: the neutron is simply gone.
				absorbedSlow += burned / Math.max(1.0E-6D, config.xenonBurnoutChance.get());
			}
		}

		// --- Parasitic capture by everything else --------------------------------
		// Any fuel block scatters and captures some neutrons regardless of composition.
		// Without this a reactor would be unrealistically easy to make critical.
		double mass = Math.max(0.0D, fuel.get(Nuclide.U238) + fuel.get(Nuclide.U235)
			+ fuel.get(Nuclide.PU239) + fuel.get(Nuclide.HIGH_WASTE));
		if (mass > 0.0D) {
			double parasitic = Math.min(slowFlux * 0.002D * (mass / FuelComposition.TOTAL_UNITS),
				slowFlux * 0.5D);
			absorbedSlow += parasitic;
		}

		return new ReactionResult(fuel.build(), released, absorbedSlow, absorbedIntermediate);
	}

	/** Distributes a number of released flux units evenly across all six faces. */
	private static void emit(FluxVector released, FluxType type, double amount, RandomSource random) {
		if (amount <= 0.0D) {
			return;
		}
		double perFace = amount / FluxType.DIRECTIONS;
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
			released.add(direction, type, perFace);
		}
	}

	/**
	 * The outcome of a neutron-reaction step.
	 *
	 * @param composition        the fuel after the step
	 * @param released           flux released by the reactions, per face
	 * @param absorbedSlow       slow neutrons consumed, used to heat adjacent water
	 * @param absorbedIntermediate intermediate neutrons consumed
	 */
	public record ReactionResult(FuelComposition composition, FluxVector released,
		double absorbedSlow, double absorbedIntermediate) {}
}