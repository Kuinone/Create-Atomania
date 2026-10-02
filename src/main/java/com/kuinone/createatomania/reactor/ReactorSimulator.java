package com.kuinone.createatomania.reactor;

import java.util.List;

import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.content.fuel.FuelBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Drives the whole reactor simulation.
 * <p>
 * The entry point is {@link #tick(Level, BlockPos)}. The sweep is deliberately two-phase:
 * every block first converts its buffered incoming flux into nuclide changes and outgoing
 * flux, and only then is that outgoing flux handed to neighbours. A single-phase sweep would
 * let the result depend on iteration order, which for a chain reaction is the difference
 * between a stable reactor and one that runs away.
 */
public final class ReactorSimulator {

	private ReactorSimulator() {}

	/**
	 * Advances one reactor by a single simulation step.
	 *
	 * @param level  the level, server side only
	 * @param origin a block belonging to the reactor, normally the fuel block that ticked
	 */
	public static void tick(Level level, BlockPos origin) {
		if (level.isClientSide()) {
			return;
		}
		RandomSource random = level.getRandom();
		AtomaniaConfig config = AtomaniaConfig.INSTANCE;

		List<FuelBlockEntity> fuels = ReactorScanner.collectFuelBlocks(level, origin);
		if (fuels.isEmpty()) {
			return;
		}

		// Phase 1: react. Each fuel block turns its buffered incoming flux into nuclide
		// changes and freshly emitted flux.
		for (FuelBlockEntity fuel : fuels) {
			fuel.simulateStep(random, config);
		}

		// Phase 2: deliver. Emitted flux crosses into the neighbours' buffers, losing
		// whatever the intervening block moderates or absorbs on the way.
		for (FuelBlockEntity fuel : fuels) {
			deliverOutgoing(level, fuel);
		}

		// Phase 3: thermal. Hot fuel bleeds heat into any adjacent water, and the water
		// sheds a little to the environment. This is what lets a reactor drive a boiler.
		if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			for (FuelBlockEntity fuel : fuels) {
				transferHeatToWater(serverLevel, fuel, config);
			}
			HeatedWaterData.coolAll(serverLevel);
			refreshDetectors(serverLevel, fuels);
		}
	}

	/**
	 * Re-samples every detector near the reactor.
	 * <p>
	 * Detectors are refreshed after the reactor has finished reacting, so their reading always
	 * reflects a completed step rather than a half-updated one.
	 */
	private static void refreshDetectors(net.minecraft.server.level.ServerLevel level,
		List<FuelBlockEntity> fuels) {

		java.util.Set<BlockPos> refreshed = new java.util.HashSet<>();
		for (FuelBlockEntity fuel : fuels) {
			BlockPos center = fuel.getBlockPos();
			for (BlockPos candidate : BlockPos.betweenClosed(
				center.offset(-DETECTOR_REACH, -DETECTOR_REACH, -DETECTOR_REACH),
				center.offset(DETECTOR_REACH, DETECTOR_REACH, DETECTOR_REACH))) {

				if (refreshed.contains(candidate) || !level.isLoaded(candidate)) {
					continue;
				}
				if (level.getBlockEntity(candidate) instanceof
					com.kuinone.createatomania.content.detector.DetectorBlockEntity detector) {
					refreshed.add(candidate.immutable());
					detector.sample();
				}
			}
		}
	}

	/** How far from a fuel block a detector may still need refreshing. */
	private static final int DETECTOR_REACH =
		com.kuinone.createatomania.content.detector.DetectorBlockEntity.RANGE + 1;

	/**
	 * Moves heat from a fuel block into adjacent water blocks.
	 * <p>
	 * This is the reactor's only route to doing useful work: the fuel cannot drive a boiler
	 * directly, it has to heat water first, and the water is what the boiler recognises as a
	 * heat source.
	 */
	private static void transferHeatToWater(net.minecraft.server.level.ServerLevel level,
		FuelBlockEntity fuel, AtomaniaConfig config) {

		double available = fuel.getHeat();
		if (available <= 0.0D) {
			return;
		}

		// Count the water neighbours first so the heat is shared evenly rather than all
		// dumped into whichever face happens to be visited last.
		java.util.List<BlockPos> waterNeighbours = new java.util.ArrayList<>(6);
		for (Direction direction : Direction.values()) {
			BlockPos neighbour = fuel.getBlockPos().relative(direction);
			if (level.isLoaded(neighbour)
				&& level.getBlockState(neighbour).is(net.minecraft.world.level.block.Blocks.WATER)) {
				waterNeighbours.add(neighbour);
			}
		}
		if (waterNeighbours.isEmpty()) {
			return;
		}

		double perBlock = config.fuelToWaterHeatTransfer.get();
		double total = perBlock * waterNeighbours.size();
		double transferred = Math.min(total, available);

		if (transferred <= 0.0D) {
			return;
		}
		fuel.addHeat(-transferred);

		double share = transferred / waterNeighbours.size();
		for (BlockPos water : waterNeighbours) {
			HeatedWaterData.addHeat(level, water, share);
		}
	}

	/**
	 * Moves one block's outgoing flux into its neighbours' incoming buffers.
	 * <p>
	 * The block being crossed is what does the moderating: water slows neutrons, a control
	 * rod eats them, and open air simply attenuates.
	 */
	private static void deliverOutgoing(Level level, FuelBlockEntity source) {
		FluxVector outgoing = source.getOutgoingFlux();
		if (outgoing.isEmpty()) {
			return;
		}
		BlockPos sourcePos = source.getBlockPos();
		AtomaniaConfig config = AtomaniaConfig.INSTANCE;
		double attenuation = config.fluxAttenuation.get();

		for (Direction direction : Direction.values()) {
			BlockPos targetPos = sourcePos.relative(direction);
			if (!level.isLoaded(targetPos)) {
				continue;
			}
			if (!(level.getBlockEntity(targetPos) instanceof FuelBlockEntity target)) {
				continue;
			}

			// The shared face: flux leaves the source through `direction` and arrives at the
			// target through the opposite face.
			Direction arrivalFace = direction.getOpposite();

			double moderation = ReactorMedium.moderationChance(level, targetPos);
			double absorption = ReactorMedium.absorptionFraction(level, targetPos, config);
			RandomSource random = level.getRandom();

			for (FluxType type : FluxType.values()) {
				double value = outgoing.get(direction, type);
				if (value <= 0.0D) {
					continue;
				}

				// Neutrons lose one energy rank with the medium's probability. Radiation is
				// unaffected by moderation but is still attenuated and absorbed.
				if (type.isNeutron() && moderation > 0.0D) {
					int slowed = Reactions.count(value * moderation, random);
					FluxType lower = type.moderated();
					if (slowed > 0 && lower != null) {
						double moved = Math.min(slowed, value);
						target.getIncomingFlux().add(arrivalFace, lower, moved * attenuation);
						value -= moved;
					}
				}

				double surviving = value * attenuation * (1.0D - Math.min(1.0D, absorption));
				if (surviving > 0.0D) {
					target.getIncomingFlux().add(arrivalFace, type, surviving);
				}
			}
		}
	}
}