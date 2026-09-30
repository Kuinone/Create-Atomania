package com.kuinone.createatomania.reactor;

import java.util.Arrays;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An immutable snapshot of the nuclide inventory of a fuel block.
 * <p>
 * Amounts are unitless "fuel units". {@link #TOTAL_UNITS} units is one full fresh fuel block.
 * Fission converts one fissile unit into one unit of waste, so the total is conserved.
 */
public final class FuelComposition {

	/** Fuel units in one freshly crafted block. */
	public static final double TOTAL_UNITS = 100.0D;

	/** Fraction of natural uranium that is U-235. */
	public static final double NATURAL_ENRICHMENT = 0.0072D;

	private static final Codec<double[]> AMOUNTS_CODEC = Codec.DOUBLE.listOf()
		.xmap(FuelComposition::toArray, FuelComposition::toList);

	/** Persistent form: a plain list of amounts in {@link Nuclide} ordinal order. */
	public static final Codec<FuelComposition> CODEC =
		AMOUNTS_CODEC.xmap(FuelComposition::new, composition -> composition.amounts);

	/** Network form: a var-int length followed by that many doubles. */
	public static final StreamCodec<RegistryFriendlyByteBuf, FuelComposition> STREAM_CODEC =
		new StreamCodec<>() {
			@Override
			public FuelComposition decode(RegistryFriendlyByteBuf buffer) {
				int length = ByteBufCodecs.VAR_INT.decode(buffer);
				double[] amounts = new double[Nuclide.values().length];
				for (int i = 0; i < length && i < amounts.length; i++) {
					amounts[i] = buffer.readDouble();
				}
				return new FuelComposition(amounts);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buffer, FuelComposition value) {
				ByteBufCodecs.VAR_INT.encode(buffer, value.amounts.length);
				for (double amount : value.amounts) {
					buffer.writeDouble(amount);
				}
			}
		};

	/** Natural uranium: {@value #NATURAL_ENRICHMENT} U-235, the rest U-238. */
	public static final FuelComposition NATURAL_URANIUM = builder()
		.with(Nuclide.U235, TOTAL_UNITS * NATURAL_ENRICHMENT)
		.with(Nuclide.U238, TOTAL_UNITS * (1.0D - NATURAL_ENRICHMENT))
		.build();

	/** An empty composition, used for recycling maths. */
	public static final FuelComposition EMPTY = new FuelComposition(new double[Nuclide.values().length]);

	private final double[] amounts;

	private FuelComposition(double[] amounts) {
		this.amounts = amounts;
	}

	private static double[] toArray(java.util.List<Double> list) {
		double[] out = new double[Nuclide.values().length];
		for (int i = 0; i < out.length && i < list.size(); i++) {
			out[i] = list.get(i);
		}
		return out;
	}

	private static java.util.List<Double> toList(double[] amounts) {
		java.util.List<Double> list = new java.util.ArrayList<>(amounts.length);
		for (double amount : amounts) {
			list.add(amount);
		}
		return list;
	}

	public static Builder builder() {
		return new Builder();
	}

	public double get(Nuclide nuclide) {
		return amounts[nuclide.ordinal()];
	}

	/** @return the sum of every nuclide in this composition. */
	public double total() {
		double sum = 0.0D;
		for (double amount : amounts) {
			sum += amount;
		}
		return sum;
	}

	/** @return total fissile material (U-235 + Pu-239). */
	public double fissileMass() {
		return get(Nuclide.U235) + get(Nuclide.PU239);
	}

	/** @return true if there is nothing left worth reacting. */
	public boolean isEmpty() {
		return total() <= 1.0E-6D;
	}

	/** @return a mutable copy of this composition. */
	public Builder toBuilder() {
		return new Builder(amounts.clone());
	}

	/** @return a new composition scaled so its total is {@link #TOTAL_UNITS}, if non-empty. */
	public FuelComposition normalised() {
		double total = total();
		if (total <= 1.0E-6D) {
			return EMPTY;
		}
		double[] scaled = new double[amounts.length];
		double factor = TOTAL_UNITS / total;
		for (int i = 0; i < amounts.length; i++) {
			scaled[i] = amounts[i] * factor;
		}
		return new FuelComposition(scaled);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FuelComposition[");
		for (Nuclide nuclide : Nuclide.values()) {
			double amount = get(nuclide);
			if (amount > 1.0E-6D) {
				if (sb.charAt(sb.length() - 1) != '[') {
					sb.append(", ");
				}
				sb.append(nuclide.getSerializedName()).append('=').append(String.format("%.3f", amount));
			}
		}
		return sb.append(']').toString();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FuelComposition composition && Arrays.equals(amounts, composition.amounts);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(amounts);
	}

	/** Mutable builder for {@link FuelComposition}. */
	public static final class Builder {
		private final double[] amounts;

		private Builder() {
			this(new double[Nuclide.values().length]);
		}

		private Builder(double[] amounts) {
			this.amounts = amounts;
		}

		public Builder with(Nuclide nuclide, double amount) {
			amounts[nuclide.ordinal()] = amount;
			return this;
		}

		public Builder add(Nuclide nuclide, double delta) {
			amounts[nuclide.ordinal()] += delta;
			return this;
		}

		public double get(Nuclide nuclide) {
			return amounts[nuclide.ordinal()];
		}

		public FuelComposition build() {
			return new FuelComposition(amounts.clone());
		}
	}
}