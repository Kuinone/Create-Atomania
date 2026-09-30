package com.kuinone.createatomania;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * All tunable numbers for the reactor simulation and the radiation system.
 * <p>
 * Every reaction probability is exposed so that packs and players can rebalance the mod
 * without touching code. Defaults aim for "plausible", not "real reactor physics".
 */
@EventBusSubscriber(modid = CreateAtomania.MODID)
public final class AtomaniaConfig {

	public static final ModConfigSpec SPEC;
	public static final AtomaniaConfig INSTANCE;

	static {
		Pair<AtomaniaConfig, ModConfigSpec> pair =
			new ModConfigSpec.Builder().configure(AtomaniaConfig::new);
		INSTANCE = pair.getLeft();
		SPEC = pair.getRight();
	}

	// ------------------------------------------------------------------
	// Simulation
	// ------------------------------------------------------------------

	/** How many ticks between two full reactor simulation steps. */
	public final ModConfigSpec.IntValue simulationInterval;

	/** Flux units that survive when travelling from one block to the next, before moderation. */
	public final ModConfigSpec.DoubleValue fluxAttenuation;

	/** Neutrons that leak away each step even in open air; keeps chains local. */
	public final ModConfigSpec.DoubleValue neutronLeak;

	/** Hard cap on any single flux entry, to bound the simulation. */
	public final ModConfigSpec.DoubleValue maxFlux;

	/** Self-ignition: base probability per step that a U-238 nucleus spawns a fast neutron. */
	public final ModConfigSpec.DoubleValue spontaneousFissionChance;

	/** Per-step decay probability of I-135 into Xe-135 (real half-life ~6.6 h). */
	public final ModConfigSpec.DoubleValue iodineDecayChance;

	/** Per-step decay probability of Xe-135 into high-level waste (real half-life ~9.2 h). */
	public final ModConfigSpec.DoubleValue xenonDecayChance;

	/** Per-step decay probability of high-level waste into low-level waste. */
	public final ModConfigSpec.DoubleValue highWasteDecayChance;

	// ------------------------------------------------------------------
	// Fission cross sections, per unit of incoming flux
	// ------------------------------------------------------------------

	/** U-235 + slow neutron -> fast neutrons, iodine, high waste, radiation. */
	public final ModConfigSpec.DoubleValue u235FissionChance;
	/** Fast neutrons released per U-235 fission. */
	public final ModConfigSpec.DoubleValue u235NeutronYield;
	/** I-135 units created per U-235 fission. */
	public final ModConfigSpec.DoubleValue u235IodineYield;

	/** U-238 + intermediate neutron -> Pu-239, high waste, radiation. */
	public final ModConfigSpec.DoubleValue u238BreedChance;
	/** Pu-239 units created per successful U-238 capture. */
	public final ModConfigSpec.DoubleValue u238PlutoniumYield;

	/** Pu-239 + slow neutron -> ultrafast neutrons, high waste, iodine, radiation. */
	public final ModConfigSpec.DoubleValue pu239FissionChance;
	/** Ultrafast neutrons released per Pu-239 fission. */
	public final ModConfigSpec.DoubleValue pu239NeutronYield;
	/** I-135 units created per Pu-239 fission. */
	public final ModConfigSpec.DoubleValue pu239IodineYield;

	/** Xe-135 + slow neutron -> high-level waste (burnout of the neutron poison). */
	public final ModConfigSpec.DoubleValue xenonBurnoutChance;

	// ------------------------------------------------------------------
	// Moderation and heat
	// ------------------------------------------------------------------

	/** Chance a neutron is moderated when passing through water. */
	public final ModConfigSpec.DoubleValue waterModerationChance;
	/** Chance a neutron is moderated when passing through coal. */
	public final ModConfigSpec.DoubleValue coalModerationChance;
	/** Heat added to water per unit of slow neutron absorbed. */
	public final ModConfigSpec.DoubleValue waterHeatPerNeutron;
	/** Heat added to water per unit of radiation absorbed. */
	public final ModConfigSpec.DoubleValue waterHeatPerRadiation;
	/** Heat transferred from a fuel block to adjacent water each step. */
	public final ModConfigSpec.DoubleValue fuelToWaterHeatTransfer;
	/** Heat a water block loses per step on its own. */
	public final ModConfigSpec.DoubleValue waterCooling;
	/** Water temperature at which it stops absorbing heat. */
	public final ModConfigSpec.DoubleValue waterMaxTemperature;
	/** Water must be at least this hot to count as a boiler heat source. */
	public final ModConfigSpec.DoubleValue boilerMinTemperature;
	/** Temperature units removed from water per boiler heat query. */
	public final ModConfigSpec.DoubleValue boilerHeatDrain;
	/** Heat level reported to a boiler at {@link #boilerMinTemperature}. */
	public final ModConfigSpec.DoubleValue boilerMinHeatLevel;
	/** Extra heat level per degree above the minimum. */
	public final ModConfigSpec.DoubleValue boilerHeatLevelPerDegree;

	// ------------------------------------------------------------------
	// Control rods
	// ------------------------------------------------------------------

	/** Fraction of neutrons absorbed per control rod segment they pass through. */
	public final ModConfigSpec.DoubleValue controlRodAbsorption;
	/** How many blocks a control rod reaches, measured from its base. */
	public final ModConfigSpec.IntValue controlRodRange;

	// ------------------------------------------------------------------
	// Radiation system
	// ------------------------------------------------------------------

	/** Sieverts absorbed per step per unit of alpha flux standing in. */
	public final ModConfigSpec.DoubleValue sievertsPerAlpha;
	/** Sieverts absorbed per step per unit of beta flux. */
	public final ModConfigSpec.DoubleValue sievertsPerBeta;
	/** Sieverts absorbed per step per unit of gamma flux. */
	public final ModConfigSpec.DoubleValue sievertsPerGamma;
	/** Sieverts absorbed per step per unit of neutron flux. */
	public final ModConfigSpec.DoubleValue sievertsPerNeutron;

	/** Dose in sieverts that decays away per second (biological clearance). */
	public final ModConfigSpec.DoubleValue doseDecayPerSecond;

	/** Lethal accumulated dose in sieverts; above this the entity dies. */
	public final ModConfigSpec.DoubleValue lethalDose;
	/** Dose in sieverts at which the first stage of radiation sickness begins. */
	public final ModConfigSpec.DoubleValue sicknessThreshold;
	/** Dose span in sieverts covered by one severity stage. */
	public final ModConfigSpec.DoubleValue dosePerStage;
	/** Maximum severity stage, capping debuff strength. */
	public final ModConfigSpec.IntValue maxSeverity;

	/** Sieverts per second added per radioactive item held in the inventory. */
	public final ModConfigSpec.DoubleValue dosePerHeldItem;
	/** Radius in blocks in which a dropped radioactive item irradiates entities. */
	public final ModConfigSpec.DoubleValue itemRadiationRadius;

	private AtomaniaConfig(ModConfigSpec.Builder builder) {
		builder.comment("Reactor simulation settings").push("simulation");

		simulationInterval = builder
			.comment("Ticks between reactor simulation steps. Lower is more accurate but costlier.")
			.defineInRange("simulationInterval", 10, 1, 200);
		fluxAttenuation = builder
			.comment("Fraction of flux that survives travelling from one block to the next.")
			.defineInRange("fluxAttenuation", 0.75D, 0.0D, 1.0D);
		neutronLeak = builder
			.comment("Fraction of neutrons lost per step to leakage, before propagation.")
			.defineInRange("neutronLeak", 0.10D, 0.0D, 1.0D);
		maxFlux = builder
			.comment("Upper bound on a single flux entry, to keep the simulation bounded.")
			.defineInRange("maxFlux", 10000.0D, 1.0D, 1.0E9D);
		spontaneousFissionChance = builder
			.comment("Per-step probability that U-238 spontaneously emits a fast neutron.")
			.defineInRange("spontaneousFissionChance", 0.0015D, 0.0D, 1.0D);
		iodineDecayChance = builder
			.comment("Per-step probability that I-135 decays into Xe-135.")
			.defineInRange("iodineDecayChance", 0.01D, 0.0D, 1.0D);
		xenonDecayChance = builder
			.comment("Per-step probability that Xe-135 decays into high-level waste.")
			.defineInRange("xenonDecayChance", 0.006D, 0.0D, 1.0D);
		highWasteDecayChance = builder
			.comment("Per-step probability that high-level waste decays into low-level waste.")
			.defineInRange("highWasteDecayChance", 0.002D, 0.0D, 1.0D);

		builder.pop().comment("Fission cross sections, all probabilities are per unit of incoming flux")
			.push("cross_sections");

		u235FissionChance = builder
			.comment("Probability that one slow neutron fissions a U-235 nucleus.")
			.defineInRange("u235FissionChance", 0.05D, 0.0D, 1.0D);
		u235NeutronYield = builder
			.comment("Fast neutrons released per U-235 fission (real average is ~2.43).")
			.defineInRange("u235NeutronYield", 2.43D, 0.0D, 10.0D);
		u235IodineYield = builder
			.comment("I-135 units created per U-235 fission.")
			.defineInRange("u235IodineYield", 0.06D, 0.0D, 1.0D);

		u238BreedChance = builder
			.comment("Probability that one intermediate neutron is captured by U-238, breeding Pu-239.")
			.defineInRange("u238BreedChance", 0.02D, 0.0D, 1.0D);
		u238PlutoniumYield = builder
			.comment("Pu-239 units created per successful U-238 neutron capture.")
			.defineInRange("u238PlutoniumYield", 1.0D, 0.0D, 1.0D);

		pu239FissionChance = builder
			.comment("Probability that one slow neutron fissions a Pu-239 nucleus.")
			.defineInRange("pu239FissionChance", 0.08D, 0.0D, 1.0D);
		pu239NeutronYield = builder
			.comment("Ultrafast neutrons released per Pu-239 fission (real average is ~2.9).")
			.defineInRange("pu239NeutronYield", 2.9D, 0.0D, 10.0D);
		pu239IodineYield = builder
			.comment("I-135 units created per Pu-239 fission.")
			.defineInRange("pu239IodineYield", 0.07D, 0.0D, 1.0D);

		xenonBurnoutChance = builder
			.comment("Probability that one slow neutron burns a Xe-135 nucleus into high-level waste.")
			.defineInRange("xenonBurnoutChance", 0.30D, 0.0D, 1.0D);

		builder.pop().comment("Neutron moderation and heat exchange").push("thermal");

		waterModerationChance = builder
			.comment("Chance a neutron is slowed one energy rank when passing through water.")
			.defineInRange("waterModerationChance", 0.55D, 0.0D, 1.0D);
		coalModerationChance = builder
			.comment("Chance a neutron is slowed one energy rank when passing through coal.")
			.defineInRange("coalModerationChance", 0.30D, 0.0D, 1.0D);
		waterHeatPerNeutron = builder
			.comment("Heat added to water per unit of slow neutron it absorbs.")
			.defineInRange("waterHeatPerNeutron", 40.0D, 0.0D, 100000.0D);
		waterHeatPerRadiation = builder
			.comment("Heat added to water per unit of radiation it absorbs.")
			.defineInRange("waterHeatPerRadiation", 6.0D, 0.0D, 100000.0D);
		fuelToWaterHeatTransfer = builder
			.comment("Heat moved from a fuel block to each adjacent water block per step.")
			.defineInRange("fuelToWaterHeatTransfer", 12.0D, 0.0D, 100000.0D);
		waterCooling = builder
			.comment("Heat a water block sheds per step on its own.")
			.defineInRange("waterCooling", 0.35D, 0.0D, 100000.0D);
		waterMaxTemperature = builder
			.comment("Temperature at which water stops absorbing more heat.")
			.defineInRange("waterMaxTemperature", 2000.0D, 1.0D, 100000.0D);
		boilerMinTemperature = builder
			.comment("Minimum water temperature for it to count as a boiler heat source.")
			.defineInRange("boilerMinTemperature", 200.0D, 0.0D, 100000.0D);
		boilerHeatDrain = builder
			.comment("Temperature removed from a water block each time a boiler draws from it.")
			.defineInRange("boilerHeatDrain", 6.0D, 0.0D, 100000.0D);
		boilerMinHeatLevel = builder
			.comment("Heat level a boiler sees at exactly boilerMinTemperature.")
			.defineInRange("boilerMinHeatLevel", 1.0D, 0.0D, 100.0D);
		boilerHeatLevelPerDegree = builder
			.comment("Additional heat level per degree above boilerMinTemperature.")
			.defineInRange("boilerHeatLevelPerDegree", 0.02D, 0.0D, 10.0D);

		builder.pop().comment("Control rods").push("control_rod");

		controlRodAbsorption = builder
			.comment("Fraction of neutrons absorbed per control rod segment traversed.")
			.defineInRange("controlRodAbsorption", 0.65D, 0.0D, 1.0D);
		controlRodRange = builder
			.comment("How many blocks a control rod reaches, counted from its base.")
			.defineInRange("controlRodRange", 8, 1, 32);

		builder.pop().comment("Radiation and radiation sickness").push("radiation");

		sievertsPerAlpha = builder
			.comment("Sieverts absorbed per step per unit of alpha flux.")
			.defineInRange("sievertsPerAlpha", 0.00008D, 0.0D, 1000.0D);
		sievertsPerBeta = builder
			.comment("Sieverts absorbed per step per unit of beta flux.")
			.defineInRange("sievertsPerBeta", 0.00004D, 0.0D, 1000.0D);
		sievertsPerGamma = builder
			.comment("Sieverts absorbed per step per unit of gamma flux.")
			.defineInRange("sievertsPerGamma", 0.00006D, 0.0D, 1000.0D);
		sievertsPerNeutron = builder
			.comment("Sieverts absorbed per step per unit of neutron flux.")
			.defineInRange("sievertsPerNeutron", 0.00012D, 0.0D, 1000.0D);
		doseDecayPerSecond = builder
			.comment("Dose in sieverts that the body clears per second.")
			.defineInRange("doseDecayPerSecond", 0.0005D, 0.0D, 100.0D);
		lethalDose = builder
			.comment("Accumulated dose in sieverts that kills an entity (real LD50/30 is ~4 Sv).")
			.defineInRange("lethalDose", 8.0D, 0.001D, 100000.0D);
		sicknessThreshold = builder
			.comment("Dose in sieverts at which radiation sickness stage 1 begins.")
			.defineInRange("sicknessThreshold", 0.25D, 0.0D, 100000.0D);
		dosePerStage = builder
			.comment("Additional dose in sieverts per severity stage.")
			.defineInRange("dosePerStage", 0.75D, 0.001D, 100000.0D);
		maxSeverity = builder
			.comment("Highest radiation sickness stage.")
			.defineInRange("maxSeverity", 6, 1, 20);
		dosePerHeldItem = builder
			.comment("Sieverts per second per radioactive item carried in the inventory.")
			.defineInRange("dosePerHeldItem", 0.0012D, 0.0D, 100.0D);
		itemRadiationRadius = builder
			.comment("Radius in blocks around a dropped radioactive item in which entities are dosed.")
			.defineInRange("itemRadiationRadius", 2.5D, 0.0D, 32.0D);

		builder.pop();
	}

	@SubscribeEvent
	static void onLoad(ModConfigEvent.Loading event) {
		CreateAtomania.LOGGER.info("Create: Atomania configuration loaded.");
	}
}