package com.kuinone.createatomania.reactor;

/**
 * Interface implemented by any block entity that holds flux.
 * <p>
 * The reactor simulation is a two-phase sweep over a reactor's blocks: first every block
 * reacts with the flux it received last step and computes what it emits, then the emitted
 * flux is delivered to the neighbours' buffers. Doing it in two phases rather than one
 * means the result does not depend on the order blocks happen to be visited in.
 */
public interface FluxHolder {

	/** @return the flux arriving on each face during the current step. */
	FluxVector getIncomingFlux();

	/** @return the flux this block emits on each face during the current step. */
	FluxVector getOutgoingFlux();

	/** Moderation probability applied to neutrons crossing this block, in [0,1]. */
	default double moderationChance() {
		return 0.0D;
	}

	/** Fraction of neutrons absorbed when crossing this block, in [0,1]. */
	default double absorptionFraction() {
		return 0.0D;
	}

	/** @return true if this block stops flux completely. */
	default boolean blocksFlux() {
		return false;
	}
}