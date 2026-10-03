package com.kuinone.createatomania;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Create: Atomania - a nuclear technology addon for Create.
 * <p>
 * The mod splits into three layers. The {@code reactor} package holds the physics and knows
 * nothing about Minecraft blocks beyond positions and levels. The {@code content} package
 * holds the blocks and items and translates the physics into game behaviour. The
 * {@code radiation} package turns flux into doses and doses into illness.
 */
@Mod(CreateAtomania.MODID)
public class CreateAtomania {

	public static final String MODID = "createatomania";
	public static final Logger LOGGER = LogUtils.getLogger();

	public CreateAtomania(IEventBus modBus, ModContainer modContainer) {
		// Registries.
		//
		// Order matters here. The content registry holds the block and item entries, and the
		// block entity types refer back to those blocks when they are built. NeoForge fires a
		// separate RegisterEvent per registry, and it fires them in the order the registers
		// were added to the bus. Registering the content first means AtomaniaContent is fully
		// initialised - and its own registers still open - before the block entity types are
		// asked to resolve their blocks. Registering them the other way round deadlocks:
		// the block entity lambda would force AtomaniaContent to initialise while the item
		// register had already been closed.
		AtomaniaRegistry.register(modBus);
		com.kuinone.createatomania.content.ContentRegistry.bootstrap();
		AtomaniaBlockEntities.BLOCK_ENTITIES.register(modBus);
		com.kuinone.createatomania.content.fuel.AtomaniaDataComponents.COMPONENTS.register(modBus);
		com.kuinone.createatomania.radiation.RadiationAttachments.ATTACHMENTS.register(modBus);
		com.kuinone.createatomania.recycling.ReprocessingRecipes.TYPES.register(modBus);
		com.kuinone.createatomania.recycling.ReprocessingRecipes.SERIALIZERS.register(modBus);

		// Gameplay hooks.
		com.kuinone.createatomania.reactor.ReactorEvents.register();
		com.kuinone.createatomania.radiation.RadiationEvents.register();
		com.kuinone.createatomania.compat.CreateCompat.register();

		modContainer.registerConfig(ModConfig.Type.COMMON, AtomaniaConfig.SPEC);
		LOGGER.info("Create: Atomania initialised.");
	}
}