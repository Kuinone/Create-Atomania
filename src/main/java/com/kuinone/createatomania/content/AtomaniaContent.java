package com.kuinone.createatomania.content;

import com.kuinone.createatomania.AtomaniaRegistry;
import com.kuinone.createatomania.content.fuel.FuelBlock;
import com.kuinone.createatomania.content.fuel.FuelBlockItem;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Every item and block the mod adds.
 * <p>
 * Activity figures are in becquerels per item and are the only place radiation strength is
 * defined; the radiation system reads them rather than hard-coding anything.
 */
public final class AtomaniaContent {

	private AtomaniaContent() {}

	// ------------------------------------------------------------------
	// Items
	// ------------------------------------------------------------------

	/** Uranium ore concentrate, mildly radioactive. */
	public static final DeferredItem<Item> RAW_URANIUM = AtomaniaRegistry.ITEMS.registerItem(
		"raw_uranium",
		properties -> new RadioactiveItem(properties, 12_000.0D, ChatFormatting.GREEN));

	/** Refined uranium metal. */
	public static final DeferredItem<Item> URANIUM_INGOT = AtomaniaRegistry.ITEMS.registerItem(
		"uranium_ingot",
		properties -> new RadioactiveItem(properties, 25_000.0D, ChatFormatting.GREEN));

	/** Separated uranium-235, the fissile isotope. */
	public static final DeferredItem<Item> URANIUM_235 = AtomaniaRegistry.ITEMS.registerItem(
		"uranium_235",
		properties -> new RadioactiveItem(properties, 80_000.0D, ChatFormatting.YELLOW));

	/** Depleted uranium-238, the fertile isotope. */
	public static final DeferredItem<Item> URANIUM_238 = AtomaniaRegistry.ITEMS.registerItem(
		"uranium_238",
		properties -> new RadioactiveItem(properties, 12_500.0D, ChatFormatting.GREEN));

	/** Plutonium-239, bred from U-238. Considerably more hazardous to handle. */
	public static final DeferredItem<Item> PLUTONIUM_239 = AtomaniaRegistry.ITEMS.registerItem(
		"plutonium_239",
		properties -> new RadioactiveItem(properties, 2_000_000.0D, ChatFormatting.RED));

	/** High-level waste: the intensely radioactive short-lived fission products. */
	public static final DeferredItem<Item> HIGH_LEVEL_WASTE = AtomaniaRegistry.ITEMS.registerItem(
		"high_level_waste",
		properties -> new RadioactiveItem(properties, 5_000_000.0D, ChatFormatting.DARK_RED));

	/** Low-level waste: the much less active long-lived remainder. */
	public static final DeferredItem<Item> LOW_LEVEL_WASTE = AtomaniaRegistry.ITEMS.registerItem(
		"low_level_waste",
		properties -> new RadioactiveItem(properties, 900.0D, ChatFormatting.GRAY));

	// ------------------------------------------------------------------
	// Blocks
	// ------------------------------------------------------------------

	/** The fuel block. Holds a full nuclide inventory in its block entity. */
	public static final DeferredBlock<FuelBlock> FUEL_BLOCK = AtomaniaRegistry.BLOCKS.registerBlock(
		"fuel_block",
		FuelBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
			.pushReaction(PushReaction.BLOCK)
			.requiresCorrectToolForDrops());

	/**
	 * The fuel block's item form. It carries the composition component, which is how a
	 * partially burned block keeps its contents through breaking and replacing.
	 */
	public static final DeferredItem<FuelBlockItem> FUEL_BLOCK_ITEM = AtomaniaRegistry.ITEMS.registerItem(
		"fuel_block",
		properties -> new FuelBlockItem(FUEL_BLOCK.get(), properties));
}