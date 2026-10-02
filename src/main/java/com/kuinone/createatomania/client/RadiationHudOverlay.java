package com.kuinone.createatomania.client;

import com.kuinone.createatomania.radiation.RadiationDose;
import com.kuinone.createatomania.radiation.RadiationField;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws the radiation readout a player sees while carrying or holding a Geiger counter.
 * <p>
 * The overlay sits in the bottom-left corner, out of the way of the hotbar and the chat, and
 * only appears when a counter is actually in hand. That keeps it a tool rather than permanent
 * screen clutter: the reading is something you look at deliberately.
 */
@EventBusSubscriber(modid = com.kuinone.createatomania.CreateAtomania.MODID, value = Dist.CLIENT)
public final class RadiationHudOverlay {

	private RadiationHudOverlay() {}

	@SubscribeEvent
	static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
			return;
		}

		// Only show the readout while a counter is held, matching how Create's own
		// goggles-driven overlays behave.
		boolean holdingGeiger = isHoldingCounter(player, true);
		boolean holdingFlux = isHoldingCounter(player, false);
		if (!holdingGeiger && !holdingFlux) {
			return;
		}

		GuiGraphics graphics = event.getGuiGraphics();

		double doseRate = 0.0D;
		double accumulated = 0.0D;
		if (player.getData(com.kuinone.createatomania.radiation.RadiationAttachments.RADIATION.get())
			instanceof com.kuinone.createatomania.radiation.RadiationData data) {
			accumulated = data.getDose();
		}

		if (player.level() != null) {
			doseRate = RadiationField.doseAt(player.level(), player.blockPosition(),
				com.kuinone.createatomania.AtomaniaConfig.INSTANCE);
		}

		int x = 6;
		int y = graphics.guiHeight() - 60;
		int line = 0;

		if (holdingGeiger) {
			drawLine(graphics, Component.translatable("createatomania.hud.dose_rate")
				.append(": ")
				.append(Component.literal(RadiationDose.formatRate(doseRate))
					.withStyle(ChatFormatting.GOLD)),
				x, y, line++, 0xFF_FFAA00);
			drawLine(graphics, Component.translatable("createatomania.hud.accumulated_dose")
				.append(": ")
				.append(Component.literal(RadiationDose.format(accumulated))
					.withStyle(severityColour(accumulated))),
				x, y, line++, 0xFF_FFFFFF);
		}

		if (holdingFlux) {
			drawLine(graphics, Component.translatable("createatomania.hud.neutron_flux")
				.append(": ")
				.append(Component.literal(String.format("%.3f", neutronFluxAt(player)))
					.withStyle(ChatFormatting.AQUA)),
				x, y, line++, 0xFF_55FFFF);
		}
	}

	/** @return total neutron flux near the player, for the flux counter readout. */
	private static double neutronFluxAt(Player player) {
		double total = 0.0D;
		net.minecraft.core.BlockPos pos = player.blockPosition();
		for (net.minecraft.core.BlockPos candidate : net.minecraft.core.BlockPos.betweenClosed(
			pos.offset(-8, -8, -8), pos.offset(8, 8, 8))) {

			if (!player.level().isLoaded(candidate)) {
				continue;
			}
			if (player.level().getBlockEntity(candidate) instanceof
				com.kuinone.createatomania.content.fuel.FuelBlockEntity fuel) {
				double distance = Math.sqrt(candidate.distSqr(pos));
				if (distance < 0.5D) {
					distance = 0.5D;
				}
				total += fuel.getLastNeutronFlux() / (distance * distance);
			}
		}
		return total;
	}

	/** @return true if the counter of the requested kind is held in either hand. */
	private static boolean isHoldingCounter(Player player, boolean geiger) {
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		return matches(main, geiger) || matches(off, geiger);
	}

	private static boolean matches(ItemStack stack, boolean geiger) {
		if (stack.isEmpty()) {
			return false;
		}
		net.minecraft.world.level.block.Block expected = geiger
			? com.kuinone.createatomania.content.ContentRegistry.GEIGER_COUNTER.get()
			: com.kuinone.createatomania.content.ContentRegistry.NEUTRON_FLUX_COUNTER.get();
		return stack.is(expected.asItem());
	}

	/** @return a colour reflecting how serious the accumulated dose is. */
	private static ChatFormatting severityColour(double dose) {
		double lethal = com.kuinone.createatomania.AtomaniaConfig.INSTANCE.lethalDose.get();
		double fraction = lethal <= 0.0D ? 0.0D : dose / lethal;
		if (fraction >= 0.75D) {
			return ChatFormatting.DARK_RED;
		}
		if (fraction >= 0.4D) {
			return ChatFormatting.RED;
		}
		if (fraction >= 0.15D) {
			return ChatFormatting.YELLOW;
		}
		return ChatFormatting.GREEN;
	}

	private static void drawLine(GuiGraphics graphics, Component text, int x, int y, int line,
		int colour) {
		graphics.drawString(Minecraft.getInstance().font, text, x, y + line * 10, colour, true);
	}
}