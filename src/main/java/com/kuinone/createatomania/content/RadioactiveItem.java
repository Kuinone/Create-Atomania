package com.kuinone.createatomania.content;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An item that is radioactive and therefore irradiates whoever carries it.
 * <p>
 * The {@link #becquerels()} value is a fixed property of the material, expressed in
 * becquerels per item (decays per second). The radiation system sums this over a player's
 * whole inventory and converts it into an absorbed dose, so carrying ten ingots irradiates
 * you ten times as fast as carrying one.
 */
public class RadioactiveItem extends Item {

	private final double becquerels;
	private final ChatFormatting accent;

	public RadioactiveItem(Properties properties, double becquerels) {
		this(properties, becquerels, ChatFormatting.GREEN);
	}

	public RadioactiveItem(Properties properties, double becquerels, ChatFormatting accent) {
		super(properties);
		this.becquerels = becquerels;
		this.accent = accent;
	}

	/** @return this material's activity in becquerels per item. */
	public double becquerels() {
		return becquerels;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {

		super.appendHoverText(stack, context, tooltip, flag);

		double held = becquerels * stack.getCount();
		tooltip.add(Component.translatable("createatomania.tooltip.activity",
				formatActivity(held))
			.withStyle(accent));

		if (becquerels > 0.0D) {
			tooltip.add(Component.translatable("createatomania.tooltip.radioactive_warning")
				.withStyle(ChatFormatting.DARK_RED));
		}
	}

	/** Formats an activity value with a sensible SI prefix. */
	public static String formatActivity(double becquerels) {
		if (becquerels >= 1.0E9D) {
			return String.format("%.2f GBq", becquerels / 1.0E9D);
		}
		if (becquerels >= 1.0E6D) {
			return String.format("%.2f MBq", becquerels / 1.0E6D);
		}
		if (becquerels >= 1.0E3D) {
			return String.format("%.2f kBq", becquerels / 1.0E3D);
		}
		return String.format("%.2f Bq", becquerels);
	}
}