package com.kuinone.createatomania.reactor;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Tracks the temperature of water blocks heated by a reactor.
 * <p>
 * Water temperature is not block state, because vanilla water has no such property and
 * replacing it with a custom fluid block would break every other mod's water handling. It is
 * kept here instead, keyed by position, and consulted by the Create boiler heater.
 * <p>
 * The store is per level and drops entries for positions that no longer hold water, so it
 * cannot grow without bound as a player builds and tears down reactors.
 */
public final class HeatedWaterData {

	private static final Map<ServerLevel, Map<BlockPos, Double>> TEMPERATURES = new WeakHashMap<>();

	private HeatedWaterData() {}

	/** @return the stored temperature, or 0 if this water has never been heated. */
	public static double getTemperature(ServerLevel level, BlockPos pos) {
		Map<BlockPos, Double> temperatures = TEMPERATURES.get(level);
		if (temperatures == null) {
			return 0.0D;
		}
		return temperatures.getOrDefault(pos, 0.0D);
	}

	/** @return true if this water is hot enough to drive a boiler. */
	public static boolean isHot(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(Blocks.WATER)) {
			return false;
		}
		return getTemperature(level, pos) >=
			com.kuinone.createatomania.AtomaniaConfig.INSTANCE.boilerMinTemperature.get();
	}

	/** Sets the temperature of a water block. */
	public static void setTemperature(ServerLevel level, BlockPos pos, double temperature) {
		Map<BlockPos, Double> temperatures = TEMPERATURES.computeIfAbsent(level,
			ignored -> new java.util.HashMap<>());
		if (temperature <= 0.0D) {
			temperatures.remove(pos);
		} else {
			temperatures.put(pos.immutable(), temperature);
		}
	}

	/** Adds heat to a water block, clamped to the configured maximum. */
	public static void addHeat(ServerLevel level, BlockPos pos, double delta) {
		double current = getTemperature(level, pos);
		double max = com.kuinone.createatomania.AtomaniaConfig.INSTANCE.waterMaxTemperature.get();
		setTemperature(level, pos, Math.min(max, current + delta));
	}

	/**
	 * Cools every tracked water block by the ambient cooling rate.
	 * <p>
	 * Called once per simulation step so that water left alone eventually returns to ambient,
	 * which stops a player from banking heat in a lake indefinitely.
	 */
	public static void coolAll(ServerLevel level) {
		Map<BlockPos, Double> temperatures = TEMPERATURES.get(level);
		if (temperatures == null || temperatures.isEmpty()) {
			return;
		}
		double cooling = com.kuinone.createatomania.AtomaniaConfig.INSTANCE.waterCooling.get();
		temperatures.entrySet().removeIf(entry -> {
			BlockPos pos = entry.getKey();
			if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.WATER)) {
				return true;
			}
			double next = entry.getValue() - cooling;
			if (next <= 0.0D) {
				return true;
			}
			entry.setValue(next);
			return false;
		});
	}

	/** Drops all state for a level. */
	public static void clear(ServerLevel level) {
		TEMPERATURES.remove(level);
	}
}