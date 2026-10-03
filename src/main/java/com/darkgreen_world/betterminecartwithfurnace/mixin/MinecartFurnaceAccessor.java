package com.darkgreen_world.betterminecartwithfurnace.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;

@Mixin(MinecartFurnace.class)
public interface MinecartFurnaceAccessor {
	@Invoker("hasFuel")
	boolean betterMinecartWithFurnace$hasFuel();

	@Invoker("setHasFuel")
	void betterMinecartWithFurnace$setHasFuel(boolean hasFuel);
}
