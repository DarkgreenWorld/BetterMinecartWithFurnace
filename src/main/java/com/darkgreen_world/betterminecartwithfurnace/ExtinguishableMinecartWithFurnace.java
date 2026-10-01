package com.darkgreen_world.betterminecartwithfurnace;

import net.minecraft.world.phys.Vec3;

/** Implemented by MinecartFurnaceMixin so that the other mixins (dispenser, minecart physics) can read and change its state. */
public interface ExtinguishableMinecartWithFurnace {
	/** Has fuel left but is not lit: put out with water, or an empty minecart that was refuelled by a hopper. */
	boolean betterMinecartWithFurnace$isExtinguished();

	/** Has fuel and is not extinguished. */
	boolean betterMinecartWithFurnace$isBurning();

	/** Puts out a burning minecart. Does nothing and returns false if it is not burning (empty or already extinguished). */
	boolean betterMinecartWithFurnace$extinguish();

	/**
	 * Lights an extinguished minecart, which continues in the direction it had before it was put out.
	 * If it has no such direction (an empty minecart refuelled by a hopper), it heads away from igniterPos,
	 * like vanilla does when fuel is added.
	 */
	void betterMinecartWithFurnace$reignite(Vec3 igniterPos);
}
