package com.kuinone.createatomania.content.detector;

import java.util.List;

import com.kuinone.createatomania.AtomaniaBlockEntities;
import com.kuinone.createatomania.AtomaniaConfig;
import com.kuinone.createatomania.content.fuel.FuelBlockEntity;
import com.kuinone.createatomania.radiation.RadiationDose;
import com.kuinone.createatomania.radiation.RadiationField;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A counter block that samples the radiation or neutron flux around it.
 * <p>
 * The detector does not need to touch a reactor. It sums what every nearby fuel block is
 * emitting and falls off with distance, so placing one across the room reads lower than one
 * pressed against the core. That is also why shielding works: a detector behind a wall reads
 * less than one with a clear line of sight.
 * <p>
 * The reading drives three things: the redstone output, the goggle overlay, and the reading a
 * player sees while carrying the block.
 */
public class DetectorBlockEntity extends BlockEntity implements IHaveGoggleInformation {

	/** How far the detector samples, in blocks. */
	public static final int RANGE = 8;

	private final DetectorKind kind;

	/** The most recent sampled reading, in flux units. */
	private double reading;

	/** The redstone strength derived from the last reading. */
	private int redstone;

	public DetectorBlockEntity(BlockPos pos, BlockState state, DetectorKind kind) {
		super(kind == DetectorKind.RADIATION
			? AtomaniaBlockEntities.GEIGER_COUNTER.get()
			: AtomaniaBlockEntities.NEUTRON_FLUX_COUNTER.get(),
			pos, state);
		this.kind = kind;
	}

	public DetectorKind getKind() {
		return kind;
	}

	/** @return the last sampled reading in flux units. */
	public double getReading() {
		return reading;
	}

	/** @return the redstone strength for the last reading. */
	public int getRedstoneStrength() {
		return redstone;
	}

	/** Samples the surrounding reactor blocks. Called once per simulation step. */
	public void sample() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		double total = 0.0D;

		for (BlockPos candidate : BlockPos.betweenClosed(
			worldPosition.offset(-RANGE, -RANGE, -RANGE),
			worldPosition.offset(RANGE, RANGE, RANGE))) {

			if (!serverLevel.isLoaded(candidate)
				|| !(serverLevel.getBlockEntity(candidate) instanceof FuelBlockEntity fuel)) {
				continue;
			}

			double emitted = emittedFor(fuel);
			if (emitted <= 0.0D) {
				continue;
			}
			double distance = Math.sqrt(candidate.distSqr(worldPosition));
			if (distance < 0.5D) {
				distance = 0.5D;
			}
			total += emitted / (distance * distance);
		}

		double next = Math.max(0.0D, total);
		int nextRedstone = kind.redstoneStrength(next);

		if (next != reading || nextRedstone != redstone) {
			reading = next;
			redstone = nextRedstone;
			setChanged();
			notifyUpdate();
		}
	}

	/** Pushes the change to neighbours and clients. */
	private void notifyUpdate() {
		if (level == null) {
			return;
		}
		BlockState state = getBlockState();
		level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
		level.sendBlockUpdated(worldPosition, state, state, 3);
	}

	/** @return the reading a fuel block contributes for this detector's kind. */
	private double emittedFor(FuelBlockEntity fuel) {
		return switch (kind) {
			case RADIATION -> fuel.getLastRadiation();
			case NEUTRON_FLUX -> fuel.getLastNeutronFlux();
		};
	}

	/** @return the dose rate at this block, for the Geiger counter's display. */
	public double getDoseRate() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return 0.0D;
		}
		return RadiationField.doseAt(serverLevel, worldPosition, AtomaniaConfig.INSTANCE);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CreateLang.translate(kind == DetectorKind.RADIATION
				? "gui.goggles.geiger_counter"
				: "gui.goggles.neutron_flux_counter")
			.forGoggles(tooltip);

		// The raw flux reading.
		CreateLang.builder()
			.add(CreateLang.translate("gui.goggles.flux_reading").style(ChatFormatting.GRAY))
			.text(ChatFormatting.GRAY, ": ")
			.add(CreateLang.text(String.format("%.3f", reading)).style(ChatFormatting.AQUA))
			.forGoggles(tooltip, 1);

		// A Geiger counter reports dose rate in real units, which is what the item is for.
		if (kind == DetectorKind.RADIATION) {
			CreateLang.builder()
				.add(CreateLang.translate("gui.goggles.dose_rate").style(ChatFormatting.GRAY))
				.text(ChatFormatting.GRAY, ": ")
				.add(CreateLang.text(RadiationDose.formatRate(getDoseRate()))
					.style(ChatFormatting.GOLD))
				.forGoggles(tooltip, 1);
		}

		// The redstone strength is what a player wires into their control circuit.
		CreateLang.builder()
			.add(CreateLang.translate("gui.goggles.redstone_output").style(ChatFormatting.GRAY))
			.text(ChatFormatting.GRAY, ": ")
			.add(CreateLang.text(Integer.toString(redstone)).style(ChatFormatting.RED))
			.forGoggles(tooltip, 1);

		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putDouble("Reading", reading);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		reading = tag.getDouble("Reading");
		redstone = kind.redstoneStrength(reading);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}
}