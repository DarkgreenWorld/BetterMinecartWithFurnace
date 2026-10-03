package com.darkgreen_world.betterminecartwithfurnace;

import com.darkgreen_world.betterminecartwithfurnace.mixin.MinecartFurnaceAccessor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.phys.Vec3;

/** The extinguished state of a minecart with furnace: it keeps its fuel and direction but does not burn. */
public final class Extinguishing {
	/** Entity tag marking an extinguished minecart with furnace. */
	public static final String TAG = BetterMinecartWithFurnace.MOD_ID + ".extinguished";

	private Extinguishing() {
	}

	public static boolean isExtinguished(Entity minecart) {
		return minecart.entityTags().contains(TAG);
	}

	/** Returns false if it was not burning. */
	public static boolean extinguish(MinecartFurnace minecart) {
		MinecartFurnaceAccessor accessor = (MinecartFurnaceAccessor) minecart;

		if (!accessor.betterMinecartWithFurnace$hasFuel()) {
			return false;
		}

		minecart.addTag(TAG);
		accessor.betterMinecartWithFurnace$setHasFuel(false);
		minecart.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + minecart.getRandom().nextFloat() * 0.4F);

		if (minecart.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CLOUD, minecart.getX(), minecart.getY() + 0.8, minecart.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
		}

		return true;
	}

	/** Keeps its previous direction; without one (fuel added by a hopper), heads away from igniterPos. */
	public static void reignite(MinecartFurnace minecart, Vec3 igniterPos) {
		minecart.removeTag(TAG);
		((MinecartFurnaceAccessor) minecart).betterMinecartWithFurnace$setHasFuel(true);

		if (minecart.push.lengthSqr() <= 1.0E-7) {
			minecart.push = minecart.position().subtract(igniterPos).horizontal();
		}
	}
}
