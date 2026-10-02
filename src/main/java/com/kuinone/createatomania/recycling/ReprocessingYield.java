package com.kuinone.createatomania.recycling;

import com.kuinone.createatomania.reactor.FuelComposition;
import com.kuinone.createatomania.reactor.Nuclide;

/**
 * The recovery yields from reprocessing one fuel block.
 * <p>
 * Reprocessing is inherently lossy: you never get all the uranium back out, and the
 * plutonium and waste fractions come out in proportion to what was actually in the fuel.
 * Keeping the maths here rather than in a recipe means the same numbers drive the crusher,
 * the washing and anything else that reprocesses fuel.
 */
public final class ReprocessingYield {

	/** Fraction of the remaining uranium recovered as usable metal. */
	private static final double URANIUM_RECOVERY = 0.85D;

	/** Fraction of the plutonium recovered. */
	private static final double PLUTONIUM_RECOVERY = 0.80D;

	/** Fraction of the waste streams recovered as waste. */
	private static final double WASTE_RECOVERY = 0.95D;

	/** Fuel units of a nuclide needed to produce one item. */
	private static final double UNITS_PER_ITEM = 10.0D;

	private ReprocessingYield() {}

	/**
	 * Computes the recoverable item counts from a fuel composition.
	 *
	 * @param composition the fuel being reprocessed
	 * @return the yields, never null
	 */
	public static Yields compute(FuelComposition composition) {
		// U-235 and U-238 are separated here because they reprocess into different products:
		// the fissile fraction is worth recovering, the depleted fraction is bulk metal.
		int u235 = itemCount(composition.get(Nuclide.U235) * URANIUM_RECOVERY);
		int u238 = itemCount(composition.get(Nuclide.U238) * URANIUM_RECOVERY);
		int pu239 = itemCount(composition.get(Nuclide.PU239) * PLUTONIUM_RECOVERY);
		int highWaste = itemCount(composition.get(Nuclide.HIGH_WASTE) * WASTE_RECOVERY);

		// Iodine and xenon are gases and short-lived respectively; they do not survive
		// reprocessing in any recoverable form, so they contribute only to the waste stream.
		double volatileRemainder = (composition.get(Nuclide.I135) + composition.get(Nuclide.XE135))
			* WASTE_RECOVERY;
		int lowWaste = itemCount(composition.get(Nuclide.LOW_WASTE) * WASTE_RECOVERY
			+ volatileRemainder);

		return new Yields(u235, u238, pu239, highWaste, lowWaste);
	}

	/** Converts a fuel-unit amount into a whole number of items, rounding down but keeping 1. */
	private static int itemCount(double units) {
		if (units <= 0.0D) {
			return 0;
		}
		return Math.max(1, (int) Math.floor(units / UNITS_PER_ITEM));
	}

	/**
	 * The recovered item counts.
	 * <p>
	 * A record rather than an array so the recipe code cannot mix up which slot is which.
	 */
	public record Yields(int uranium235, int uranium238, int plutonium239, int highLevelWaste,
		int lowLevelWaste) {

		/** @return true if nothing at all was recovered, i.e. the fuel was fully spent. */
		public boolean isEmpty() {
			return uranium235 == 0 && uranium238 == 0 && plutonium239 == 0
				&& highLevelWaste == 0 && lowLevelWaste == 0;
		}

		/** @return the total number of item stacks this yield produces. */
		public int totalItems() {
			return uranium235 + uranium238 + plutonium239 + highLevelWaste + lowLevelWaste;
		}
	}
}