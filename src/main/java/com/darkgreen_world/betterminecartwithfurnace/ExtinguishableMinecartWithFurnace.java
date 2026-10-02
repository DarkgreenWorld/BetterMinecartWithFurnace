package com.darkgreen_world.betterminecartwithfurnace;

import net.minecraft.world.phys.Vec3;

/** Implemented by MinecartFurnaceMixin. */
public interface ExtinguishableMinecartWithFurnace {
	/** Has fuel but is not lit. */
	boolean betterMinecartWithFurnace$isExtinguished();

	/** Has fuel and is lit. */
	boolean betterMinecartWithFurnace$isBurning();

	/** Returns false if it was not burning. */
	boolean betterMinecartWithFurnace$extinguish();

	/** Keeps its previous direction; without one, heads away from igniterPos. */
	void betterMinecartWithFurnace$reignite(Vec3 igniterPos);
}
