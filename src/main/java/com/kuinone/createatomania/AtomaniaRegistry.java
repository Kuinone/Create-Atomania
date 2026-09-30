package com.kuinone.createatomania;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
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

	private AtomaniaRegistry() {}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		BLOCKS.register(modBus);
		TABS.register(modBus);
	}
}