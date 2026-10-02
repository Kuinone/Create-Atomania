package com.kuinone.createatomania.compat;

import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.reactor.HeatedWaterData;
import com.simibubi.create.api.boiler.BoilerHeater;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Teaches Create's boilers that reactor-heated water is a heat source.
 * <p>
 * Create finds a boiler's heat by querying the blocks around it through
 * {@link BoilerHeater#findHeat}. Vanilla water is not a heater, so the mod registers a heater
 * that answers for any water block, returning a heat level proportional to how hot that water
 * has become. The boiler then consumes the heat, which is why drawing from a block cools it
 * and why a reactor has to keep running to keep the boiler fed.
 * <p>
 * Registering on the water block itself rather than on the reactor means the player's build is
 * unconstrained: the boiler does not need to touch the fuel, only the heated water, and the
 * water only has to be nearby.
 */
public final class CreateCompat {

	/** The heater instance registered for vanilla water. */
	private static final BoilerHeater REACTOR_WATER_HEATER = CreateCompat::heatOfWater;

	private static boolean registered;

	private CreateCompat() {}

	/** Registers the boiler heater. Safe to call more than once. */
	public static void register() {
		if (registered) {
			return;
		}
		registered = true;
		BoilerHeater.REGISTRY.register(Blocks.WATER, REACTOR_WATER_HEATER);
		com.kuinone.createatomania.CreateAtomania.LOGGER
			.info("Registered heated-water boiler heater for Create.");
	}

	/**
	 * Reports the heat a water block provides to an adjacent boiler.
	 *
	 * @return the heat level, or {@link BoilerHeater#NO_HEAT} if the water is cold
	 */
	private static float heatOfWater(Level level, BlockPos pos, BlockState state) {
		if (!(level instanceof ServerLevel serverLevel)) {
			// Client side there is no temperature store, so report no heat rather than
			// guessing; the boiler's visuals are driven by its own logic anyway.
			return BoilerHeater.NO_HEAT;
		}
		if (!state.is(Blocks.WATER)) {
			return BoilerHeater.NO_HEAT;
		}

		AtomaniaConfig config = AtomaniaConfig.INSTANCE;
		double temperature = HeatedWaterData.getTemperature(serverLevel, pos);
		double minimum = config.boilerMinTemperature.get();
		if (temperature < minimum) {
			return BoilerHeater.NO_HEAT;
		}

		// Drawing heat from the water is what makes the reactor's output finite: a boiler
		// with no reactor behind it quickly drains the water back to cold.
		double drain = config.boilerHeatDrain.get();
		HeatedWaterData.addHeat(serverLevel, pos, -drain);

		double level2 = config.boilerMinHeatLevel.get()
			+ (temperature - minimum) * config.boilerHeatLevelPerDegree.get();
		return (float) Math.max(1.0D, level2);
	}
}