package com.kuinone.createatomania.content.fuel;

import java.util.List;

import com.kuinone.createatomania.reactor.FuelComposition;
import com.kuinone.createatomania.reactor.Nuclide;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The item form of a fuel block.
 * <p>
 * Two things matter here. First, when placed it pushes the carried composition into the new
 * block entity, so a block retains exactly the nuclides it had when it was broken. Second,
 * its tooltip reports the current inventory as percentages, which lets a player inspect a
 * fuel block without needing the goggles.
 */
public class FuelBlockItem extends BlockItem {

	public FuelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	/**
	 * Restores the carried composition into the freshly placed block entity.
	 * <p>
	 * This runs after the block entity exists, which is why the composition is handed over
	 * here rather than at placement-state time.
	 */
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player,
		ItemStack stack, BlockState state) {

		boolean handled = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
		if (level.getBlockEntity(pos) instanceof FuelBlockEntity fuel) {
			fuel.setComposition(AtomaniaDataComponents.readComposition(stack));
			return true;
		}
		return handled;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {

		super.appendHoverText(stack, context, tooltip, flag);

		FuelComposition composition = AtomaniaDataComponents.readComposition(stack);
		double total = composition.total();
		if (total <= 0.0D) {
			return;
		}

		tooltip.add(Component.translatable("createatomania.tooltip.fuel_contents")
			.withStyle(ChatFormatting.GRAY));

		for (Nuclide nuclide : Nuclide.values()) {
			double amount = composition.get(nuclide);
			if (amount <= 0.0001D) {
				continue;
			}
			double percent = amount / total * 100.0D;
			tooltip.add(Component.literal(" ")
				.append(Component.translatable(nuclideTranslationKey(nuclide)))
				.append(Component.literal(": " + String.format("%.3f%%", percent)))
				.withStyle(nuclideColour(nuclide)));
		}
	}

	/** Translation key for a nuclide's display name. */
	public static String nuclideTranslationKey(Nuclide nuclide) {
		return "createatomania.nuclide." + nuclide.getSerializedName();
	}

	/** Chat colour used for each nuclide in tooltips and goggle readouts. */
	public static ChatFormatting nuclideColour(Nuclide nuclide) {
		return switch (nuclide) {
			case U235 -> ChatFormatting.GREEN;
			case U238 -> ChatFormatting.DARK_GREEN;
			case PU239 -> ChatFormatting.RED;
			case I135 -> ChatFormatting.YELLOW;
			case XE135 -> ChatFormatting.LIGHT_PURPLE;
			case HIGH_WASTE -> ChatFormatting.DARK_RED;
			case LOW_WASTE -> ChatFormatting.GRAY;
		};
	}
}