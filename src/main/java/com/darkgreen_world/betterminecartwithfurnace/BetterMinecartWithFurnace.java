package com.darkgreen_world.betterminecartwithfurnace;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterMinecartWithFurnace implements ModInitializer {
	public static final String MOD_ID = "better_minecart_with_furnace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModConfig.load();
		MinecartWithFurnaceInteractions.register();
		DispenserBehaviors.register();
	}
}
