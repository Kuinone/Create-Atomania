package com.kuinone.createatomania.content.detector;

import com.kuinone.createatomania.reactor.FluxType;

/**
 * Which quantity a detector block measures.
 * <p>
 * Both counter blocks share all their machinery; they differ only in what they sample. Keeping
 * this as an enum rather than two unrelated classes means the detector logic is written once
 * and stays consistent between the two blocks.
 */
public enum DetectorKind {

	/** Measures ionising radiation, i.e. alpha plus beta plus gamma. */
	RADIATION("geiger_counter"),

	/** Measures neutron flux of every energy. */
	NEUTRON_FLUX("neutron_flux_counter");

	private final String blockName;

	DetectorKind(String blockName) {
		this.blockName = blockName;
	}

	/** @return the registry name of the block using this kind. */
	public String blockName() {
		return blockName;
	}

	/**
	 * Converts a sampled reading into a redstone signal strength.
	 * <p>
	 * The mapping is logarithmic because real counters span many orders of magnitude: a
	 * linear scale would either saturate instantly near a running reactor or read zero
	 * everywhere else. The result is clamped to the vanilla 0-15 range.
	 *
	 * @param reading the raw reading in flux units
	 * @return a redstone strength from 0 to 15
	 */
	public int redstoneStrength(double reading) {
		if (reading <= 0.0D) {
			return 0;
		}
		// Roughly one strength step per doubling, saturating around a thousand flux units.
		double scaled = Math.log1p(reading) / Math.log(2.0D);
		return (int) Math.max(0, Math.min(15, Math.round(scaled)));
	}

	/** @return the translation key for this detector's readout label. */
	public String labelKey() {
		return "createatomania.detector." + name().toLowerCase(java.util.Locale.ROOT);
	}

	/** @return the flux kinds this detector sums. */
	public FluxType[] measuredTypes() {
		return switch (this) {
			case RADIATION -> FluxType.RADIATIONS;
			case NEUTRON_FLUX -> FluxType.NEUTRONS;
		};
	}
}