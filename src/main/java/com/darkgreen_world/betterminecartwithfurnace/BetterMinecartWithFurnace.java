package com.darkgreen_world.betterminecartwithfurnace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterMinecartWithFurnace implements ModInitializer {
	public static final String MOD_ID = "better_minecart_with_furnace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public interface Engine {
		/** Called only while the engine is on, after applyNaturalSlowdown. */
		void betterMinecartWithFurnace$setEngineVelocity(Vec3 velocity);
	}

	@Override
	public void onInitialize() {
		ModConfig.load();
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> ModConfig.load());
		MinecartWithFurnaceInteractions.register();
		DispenserBehaviors.register();
	}
}
