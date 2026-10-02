package com.kuinone.createatomania.radiation;

import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.reactor.FluxType;

/**
 * Converts reactor radiation flux into an absorbed dose in real units.
 * <p>
 * The mod reports dose in sieverts, the SI unit of equivalent dose actually used in radiation
 * protection, and activity in becquerels. The conversion is intentionally simple: each flux
 * kind contributes dose at a fixed rate per unit of flux, weighted by how damaging that kind
 * of radiation is. Alpha particles are weighted far more heavily than beta or gamma because
 * of their much higher linear energy transfer.
 * <p>
 * The absolute scale is chosen so that a modest unshielded reactor gives a player a
 * meaningful dose over a few minutes without instantly killing them, which is what the
 * defaults in {@link AtomaniaConfig} are tuned for.
 */
public final class RadiationDose {

	private RadiationDose() {}

	/**
	 * Converts one step's worth of incident flux into sieverts.
	 *
	 * @param alpha          alpha flux
	 * @param beta           beta flux
	 * @param gamma          gamma flux
	 * @param neutrons       total neutron flux of every energy
	 * @param config         the configuration
	 * @return the dose in sieverts for this step, never negative
	 */
	public static double fromFlux(double alpha, double beta, double gamma, double neutrons,
		AtomaniaConfig config) {

		double dose = 0.0D;
		dose += alpha * config.sievertsPerAlpha.get();
		dose += beta * config.sievertsPerBeta.get();
		dose += gamma * config.sievertsPerGamma.get();
		dose += neutrons * config.sievertsPerNeutron.get();
		return Math.max(0.0D, dose);
	}

	/** Rates used for one flux kind, in sieverts per step per unit of flux. */
	public static double perUnit(FluxType type, AtomaniaConfig config) {
		return switch (type) {
			case ALPHA -> config.sievertsPerAlpha.get();
			case BETA -> config.sievertsPerBeta.get();
			case GAMMA -> config.sievertsPerGamma.get();
			default -> config.sievertsPerNeutron.get();
		};
	}

	/**
	 * Formats a dose in sieverts with a sensible SI prefix.
	 * <p>
	 * Real doses span an enormous range, from microsieverts during a dental x-ray to whole
	 * sieverts in a serious accident, so a fixed unit would be unreadable.
	 */
	public static String format(double sieverts) {
		double abs = Math.abs(sieverts);
		if (abs >= 1.0D) {
			return String.format("%.3f Sv", sieverts);
		}
		if (abs >= 1.0E-3D) {
			return String.format("%.3f mSv", sieverts * 1.0E3D);
		}
		if (abs >= 1.0E-6D) {
			return String.format("%.3f uSv", sieverts * 1.0E6D);
		}
		return String.format("%.3f nSv", sieverts * 1.0E9D);
	}

	/**
	 * Formats a dose rate in sieverts per second.
	 * <p>
	 * Rate is what a Geiger counter really shows, so it is reported separately from the
	 * accumulated dose.
	 */
	public static String formatRate(double sievertsPerSecond) {
		return format(sievertsPerSecond) + "/s";
	}
}