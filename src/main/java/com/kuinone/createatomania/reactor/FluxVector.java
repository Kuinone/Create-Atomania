package com.kuinone.createatomania.reactor;

import net.minecraft.core.Direction;

/**
 * A six-sided vector of flux values.
 * <p>
 * Flux is stored per block face, so a reactor can be modelled directionally: a fuel block
 * emits into the face it points at, and only that neighbour receives it. Values are
 * non-negative; there is no anti-flux.
 * <p>
 * Backed by a flat {@code double[6 * 7]} array laid out as {@code direction * TYPE_COUNT + type}
 * so a whole vector can be walked without allocation.
 */
public final class FluxVector {

	private final double[] values;

	public FluxVector() {
		this.values = new double[FluxType.LENGTH];
	}

	private FluxVector(double[] values) {
		this.values = values;
	}

	public static FluxVector copyOf(FluxVector other) {
		return new FluxVector(other.values.clone());
	}

	/** Index helper: the slot for one flux type on one face. */
	public static int index(Direction direction, FluxType type) {
		return direction.ordinal() * FluxType.TYPE_COUNT + type.ordinal();
	}

	public double get(Direction direction, FluxType type) {
		return values[index(direction, type)];
	}

	public void set(Direction direction, FluxType type, double value) {
		values[index(direction, type)] = Math.max(0.0D, value);
	}

	public void add(Direction direction, FluxType type, double delta) {
		int i = index(direction, type);
		values[i] = Math.max(0.0D, values[i] + delta);
	}

	public void scale(Direction direction, FluxType type, double factor) {
		int i = index(direction, type);
		values[i] = Math.max(0.0D, values[i] * factor);
	}

	/** @return the sum of one flux type across all six faces. */
	public double total(FluxType type) {
		double sum = 0.0D;
		for (Direction direction : Direction.values()) {
			sum += get(direction, type);
		}
		return sum;
	}

	/** @return the sum of every neutron across all six faces. */
	public double totalNeutrons() {
		double sum = 0.0D;
		for (FluxType type : FluxType.NEUTRONS) {
			sum += total(type);
		}
		return sum;
	}

	/** @return the sum of every radiation kind across all six faces. */
	public double totalRadiation() {
		double sum = 0.0D;
		for (FluxType type : FluxType.RADIATIONS) {
			sum += total(type);
		}
		return sum;
	}

	/** @return the total of one type on one face, for directional readouts. */
	public double faceTotal(Direction direction) {
		double sum = 0.0D;
		for (FluxType type : FluxType.values()) {
			sum += get(direction, type);
		}
		return sum;
	}

	/**
	 * Clamps every entry to the configured maximum and drops denormal noise, keeping
	 * long-running reactors numerically stable.
	 */
	public void clampAll(double max) {
		for (int i = 0; i < values.length; i++) {
			double v = values[i];
			if (v < 1.0E-9D) {
				values[i] = 0.0D;
			} else if (v > max) {
				values[i] = max;
			}
		}
	}

	/** @return true if nothing at all is flowing through this vector. */
	public boolean isEmpty() {
		for (double v : values) {
			if (v > 0.0D) {
				return false;
			}
		}
		return true;
	}

	/** Removes all flux. */
	public void clear() {
		java.util.Arrays.fill(values, 0.0D);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Flux[");
		for (FluxType type : FluxType.values()) {
			double t = total(type);
			if (t > 1.0E-6D) {
				if (sb.charAt(sb.length() - 1) != '[') {
					sb.append(' ');
				}
				sb.append(type.getSerializedName()).append('=').append(String.format("%.4f", t));
			}
		}
		return sb.append(']').toString();
	}
}