package com.kuinone.createatomania;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry hub.
 * <p>
 * Vanilla NeoForge {@link DeferredRegister}s are used throughout rather than Create's
 * Registrate, so the addon only depends on Create's public API surface. Create interop
 * happens through the API interfaces the block entities implement.
 */
public final class AtomaniaRegistry {

	public static final DeferredRegister.Items ITEMS =
		DeferredRegister.createItems(CreateAtomania.MODID);

	public static final DeferredRegister.Blocks BLOCKS =
		DeferredRegister.createBlocks(CreateAtomania.MODID);

	public static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAtomania.MODID);

	/** The mod's creative tab, ordered after Create's own where possible. */
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = TABS.register("main",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.createatomania"))
			.icon(() -> new ItemStack(
				com.kuinone.createatomania.content.ContentRegistry.FUEL_BLOCK_ITEM.get()))
			.displayItems((parameters, output) -> {
				output.accept(com.kuinone.createatomania.content.ContentRegistry.RAW_URANIUM.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.URANIUM_INGOT.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.URANIUM_235.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.URANIUM_238.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.PLUTONIUM_239.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.HIGH_LEVEL_WASTE.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.LOW_LEVEL_WASTE.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.FUEL_BLOCK_ITEM.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.CONTROL_ROD_ITEM.get());
				output.accept(com.kuinone.createatomania.content.ContentRegistry.GEIGER_COUNTER_ITEM.get());
				output.accept(
					com.kuinone.createatomania.content.ContentRegistry.NEUTRON_FLUX_COUNTER_ITEM.get());
			})
			.build());

	private AtomaniaRegistry() {}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		BLOCKS.register(modBus);
		TABS.register(modBus);
	}
}