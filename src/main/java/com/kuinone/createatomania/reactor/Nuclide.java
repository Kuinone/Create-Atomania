package com.kuinone.createatomania.reactor;

import net.minecraft.util.StringRepresentable;

/**
 * The nuclides tracked inside a fuel block.
 * <p>
 * Values are stored as amounts, not percentages. A freshly crafted fuel block holds
 * {@link FuelComposition#TOTAL_UNITS} units split between U-235 and U-238, which matches
 * natural uranium's ~0.72% U-235 enrichment.
 */
public enum Nuclide implements StringRepresentable {
	/** Fissile, thermal-neutron driven. The actual fuel. */
	U235("u235", 0.0072D, true),
	/** Fertile, breeds into Pu-239 under intermediate neutrons. The bulk of natural uranium. */
	U238("u238", 0.9928D, false),
	/** Fissile, produced by breeding U-238. Burns hotter than U-235. */
	PU239("pu239", 0.0D, true),
	/** Iodine-135: a fission product with a ~6.6 h half-life that decays into Xe-135. */
	I135("i135", 0.0D, false),
	/** Xenon-135: the notorious neutron poison, ~9.2 h half-life, huge thermal cross-section. */
	XE135("xe135", 0.0D, false),
	/** High-level waste: short-lived, intensely radioactive fission products. */
	HIGH_WASTE("high_waste", 0.0D, false),
	/** Low-level waste: the long-lived, much less active remainder. */
	LOW_WASTE("low_waste", 0.0D, false);

	private final String name;
	/** Natural abundance in terrestrial uranium, or 0 for anything not found in ore. */
	private final double naturalAbundance;
	/** Whether this nuclide can sustain a fission chain reaction. */
	private final boolean fissile;

	Nuclide(String name, double naturalAbundance, boolean fissile) {
		this.name = name;
		this.naturalAbundance = naturalAbundance;
		this.fissile = fissile;
	}

	public double naturalAbundance() {
		return naturalAbundance;
	}

	public boolean isFissile() {
		return fissile;
	}

	/** @return true for the two waste fractions, which are not recoverable metal. */
	public boolean isWaste() {
		return this == HIGH_WASTE || this == LOW_WASTE;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}