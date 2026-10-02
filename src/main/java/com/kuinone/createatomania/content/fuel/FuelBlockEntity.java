package com.kuinone.createatomania.content.fuel;

import java.util.List;

import com.kuinone.createatomania.AtomaniaBlockEntities;
import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.reactor.FluxHolder;
import com.kuinone.createatomania.reactor.FluxType;
import com.kuinone.createatomania.reactor.FluxVector;
import com.kuinone.createatomania.reactor.FuelComposition;
import com.kuinone.createatomania.reactor.NeutronReactions;
import com.kuinone.createatomania.reactor.Nuclide;
import com.kuinone.createatomania.reactor.SpontaneousDecay;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries a fuel block's nuclide inventory and its accumulated heat.
 * <p>
 * All state is persisted to NBT. That persistence is what makes breaking and replacing a
 * fuel block lossless: the composition travels with the block item and comes back when it
 * is placed down again.
 * <p>
 * The class implements Create's {@link IHaveGoggleInformation} so that looking at a fuel
 * block through the engineer's goggles reports its full nuclide inventory.
 */
public class FuelBlockEntity extends BlockEntity implements IHaveGoggleInformation, FluxHolder {

	/** The default inventory of a freshly crafted block: natural uranium. */
	private FuelComposition composition = FuelComposition.NATURAL_URANIUM;

	/** Accumulated heat, which drives transfer into adjacent water. */
	private double heat;

	/** Flux that arrived on each face during the current step. */
	private final FluxVector incomingFlux = new FluxVector();

	/** Flux this block emits on each face during the current step. */
	private final FluxVector outgoingFlux = new FluxVector();

	/** Radiation this block released during the last completed step, for the detectors. */
	private double lastRadiation;

	/** Neutron flux through this block during the last completed step, for the detectors. */
	private double lastNeutronFlux;

	public FuelBlockEntity(BlockPos pos, BlockState state) {
		super(AtomaniaBlockEntities.FUEL_BLOCK.get(), pos, state);
	}

	@Override
	public FluxVector getIncomingFlux() {
		return incomingFlux;
	}

	@Override
	public FluxVector getOutgoingFlux() {
		return outgoingFlux;
	}

	/** @return total radiation released last step, in flux units. */
	public double getLastRadiation() {
		return lastRadiation;
	}

	/** @return total neutron flux passing through last step, in flux units. */
	public double getLastNeutronFlux() {
		return lastNeutronFlux;
	}

	/**
	 * Runs one simulation step for this block.
	 * <p>
	 * The order matters. Spontaneous decay always happens, because it needs no neutrons.
	 * The neutron-driven reactions then consume whatever flux arrived, and finally the two
	 * sets of released flux are merged into this block's outgoing buffer and the incoming
	 * buffer is cleared ready for the next step.
	 */
	public void simulateStep(RandomSource random, AtomaniaConfig config) {
		double absorbedNeutrons = incomingFlux.totalNeutrons();

		SpontaneousDecay.StepResult spontaneous = SpontaneousDecay.apply(composition, random, config);
		NeutronReactions.ReactionResult induced =
			NeutronReactions.apply(spontaneous.composition(), incomingFlux, random, config);

		composition = induced.composition();

		outgoingFlux.clear();
		mergeFlux(outgoingFlux, spontaneous.released());
		mergeFlux(outgoingFlux, induced.released());

		// Moderation and leakage apply to the neutrons as they leave this block.
		double leak = config.neutronLeak.get();
		for (FluxType type : FluxType.NEUTRONS) {
			for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
				outgoingFlux.scale(direction, type, 1.0D - leak);
			}
		}
		outgoingFlux.clampAll(config.maxFlux.get());

		// Fission heats the block, which then bleeds into any adjacent water.
		double generated = induced.absorbedSlow() * 0.05D + absorbedNeutrons * 0.01D;
		addHeat(generated);

		// Record what a detector standing next to this block would see.
		lastNeutronFlux = outgoingFlux.totalNeutrons();
		lastRadiation = outgoingFlux.totalRadiation();

		incomingFlux.clear();
		setChanged();
	}

	/** Adds every entry of {@code delta} into {@code target}. */
	private static void mergeFlux(FluxVector target, FluxVector delta) {
		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
			for (FluxType type : FluxType.values()) {
				double value = delta.get(direction, type);
				if (value > 0.0D) {
					target.add(direction, type, value);
				}
			}
		}
	}

	public FuelComposition getComposition() {
		return composition;
	}

	public void setComposition(FuelComposition composition) {
		this.composition = composition;
		setChanged();
	}

	public double getHeat() {
		return heat;
	}

	public void setHeat(double heat) {
		this.heat = Math.max(0.0D, heat);
	}

	public void addHeat(double delta) {
		setHeat(this.heat + delta);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		FuelComposition.CODEC
			.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), composition)
			.resultOrPartial(error -> com.kuinone.createatomania.CreateAtomania.LOGGER
				.warn("Failed to save fuel composition: {}", error))
			.ifPresent(encoded -> tag.put("Composition", encoded));
		tag.putDouble("Heat", heat);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		Tag encoded = tag.get("Composition");
		if (encoded != null) {
			FuelComposition.CODEC
				.parse(registries.createSerializationContext(NbtOps.INSTANCE), encoded)
				.resultOrPartial(error -> com.kuinone.createatomania.CreateAtomania.LOGGER
					.warn("Failed to load fuel composition: {}", error))
				.ifPresent(loaded -> composition = loaded);
		}
		heat = tag.getDouble("Heat");
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	/**
	 * Reports the fuel block's inventory to Create's engineer's goggles.
	 * <p>
	 * Each nuclide is shown as a percentage of the block's total, which is what actually
	 * matters to a player: how enriched the fuel is, how much plutonium has bred, and how
	 * much xenon poison has built up.
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		double total = composition.total();
		if (total <= 0.0D) {
			return false;
		}

		CreateLang.translate("gui.goggles.fuel_block")
			.forGoggles(tooltip);

		for (Nuclide nuclide : Nuclide.values()) {
			double amount = composition.get(nuclide);
			if (amount <= 0.0001D) {
				continue;
			}
			double percent = amount / total * 100.0D;

			CreateLang.builder()
				.add(CreateLang.translate(FuelBlockItem.nuclideTranslationKey(nuclide))
					.style(FuelBlockItem.nuclideColour(nuclide)))
				.text(ChatFormatting.GRAY, ": ")
				.add(CreateLang.text(String.format("%.3f%%", percent))
					.style(ChatFormatting.AQUA))
				.forGoggles(tooltip, 1);
		}

		// Fissile fraction is the single most useful derived number, so it gets its own line.
		double fissile = composition.fissileMass() / total * 100.0D;
		CreateLang.builder()
			.add(CreateLang.translate("gui.goggles.fissile_fraction")
				.style(ChatFormatting.GRAY))
			.text(ChatFormatting.GRAY, ": ")
			.add(CreateLang.text(String.format("%.3f%%", fissile))
				.style(ChatFormatting.GOLD))
			.forGoggles(tooltip, 1);

		return true;
	}
}