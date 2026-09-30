package com.kuinone.createatomania.reactor;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * The seven flux kinds simulated by the reactor.
 * <p>
 * Four of them are neutrons, ordered from highest to lowest energy. Water and coal moderate
 * them, i.e. step them one rank down this ladder. The remaining three are ionising radiation
 * emitted as a by-product of fission and decay.
 */
public enum FluxType implements StringRepresentable {
	/** ~14 MeV fusion/fission-spectrum neutrons. */
	ULTRAFAST_NEUTRON("ultrafast_neutron", Kind.NEUTRON),
	/** ~1 MeV neutrons, capable of fast fission. */
	FAST_NEUTRON("fast_neutron", Kind.NEUTRON),
	/** ~1 keV neutrons, the resonance region where U-238 breeds. */
	INTERMEDIATE_NEUTRON("intermediate_neutron", Kind.NEUTRON),
	/** Thermal (~0.025 eV) neutrons, the ones that actually sustain a light-water reactor. */
	SLOW_NEUTRON("slow_neutron", Kind.NEUTRON),
	/** Alpha radiation: heavy, short range, stopped by a sheet of paper. */
	ALPHA("alpha", Kind.RADIATION),
	/** Beta radiation: electrons, stopped by a few mm of aluminium. */
	BETA("beta", Kind.RADIATION),
	/** Gamma radiation: deeply penetrating photons, needs lead or concrete. */
	GAMMA("gamma", Kind.RADIATION);

	/** Broad classification of a flux. */
	public enum Kind {
		NEUTRON,
		RADIATION
	}

	public static final Codec<FluxType> CODEC = StringRepresentable.fromEnum(FluxType::values);

	/** Neutrons in descending energy order; index 0 is the fastest. */
	public static final FluxType[] NEUTRONS = {
		ULTRAFAST_NEUTRON, FAST_NEUTRON, INTERMEDIATE_NEUTRON, SLOW_NEUTRON
	};

	/** The three ionising radiation kinds. */
	public static final FluxType[] RADIATIONS = { ALPHA, BETA, GAMMA };

	/** The number of flux entries in a full six-sided flux vector. */
	public static final int TYPE_COUNT = values().length;
	/** Six block faces. */
	public static final int DIRECTIONS = 6;
	/** Total length of a flattened side-by-type flux array. */
	public static final int LENGTH = TYPE_COUNT * DIRECTIONS;

	private final String name;
	private final Kind kind;

	FluxType(String name, Kind kind) {
		this.name = name;
		this.kind = kind;
	}

	/** @return true if this flux is one of the four neutron energy ranks. */
	public boolean isNeutron() {
		return kind == Kind.NEUTRON;
	}

	/** @return true if this flux is alpha, beta or gamma radiation. */
	public boolean isRadiation() {
		return kind == Kind.RADIATION;
	}

	/**
	 * The next lower neutron energy rank, or {@code null} if this is not a neutron or is
	 * already fully thermalised. Used for moderation by water and coal.
	 */
	public FluxType moderated() {
		if (!isNeutron()) {
			return null;
		}
		int next = ordinal() + 1;
		return next < NEUTRONS.length ? NEUTRONS[next] : null;
	}

	/**
	 * Relative biological harm per unit of flux, used when converting radiation flux into a
	 * dose. Alpha is by far the most damaging per unit energy once internalised, gamma is the
	 * most penetrating at range. Loosely follows the ICRP radiation weighting factors.
	 */
	public double biologicalWeight() {
		return switch (this) {
			case ALPHA -> 20.0D;
			case BETA -> 1.0D;
			case GAMMA -> 1.0D;
			default -> 0.0D;
		};
	}

	/**
	 * Relative energy carried by one unit of this flux, normalised so that a thermal neutron
	 * is 1.0. Fast fission neutrons are worth far more than thermal ones.
	 */
	public double energy() {
		return switch (this) {
			case ULTRAFAST_NEUTRON -> 8.0D;
			case FAST_NEUTRON -> 4.0D;
			case INTERMEDIATE_NEUTRON -> 2.0D;
			case SLOW_NEUTRON -> 1.0D;
			case ALPHA -> 6.0D;
			case BETA -> 2.0D;
			case GAMMA -> 3.0D;
		};
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}