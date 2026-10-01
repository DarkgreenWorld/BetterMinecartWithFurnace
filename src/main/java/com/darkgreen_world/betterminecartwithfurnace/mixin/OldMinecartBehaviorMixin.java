package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehavior {
	protected OldMinecartBehaviorMixin(AbstractMinecart minecart) {
		super(minecart);
	}

	/**
	 * The {@code state.is(Blocks.POWERED_RAIL)} check at the top of moveAlongTrack decides both boosting (powerTrack)
	 * and braking (haltTrack). For a burning minecart with furnace, an active powered rail is treated as a plain rail
	 * (no boost) while an inactive one still brakes. Empty and extinguished ones are left alone and can still be
	 * launched by powered rails.
	 * is(T) is a generic default method of TypedInstance, so its erased parameter is Object; only one of the two
	 * {@code @At}s will match.
	 */
	@ModifyExpressionValue(method = "moveAlongTrack", at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"),
			@At(value = "INVOKE", target = "Lnet/minecraft/core/TypedInstance;is(Ljava/lang/Object;)Z")
	})
	private boolean betterMinecartWithFurnace$ignoreActivePoweredRail(boolean isPoweredRail) {
		if (isPoweredRail && this.minecart instanceof ExtinguishableMinecartWithFurnace furnace && furnace.betterMinecartWithFurnace$isBurning()) {
			BlockState state = this.level().getBlockState(this.minecart.getCurrentBlockPosOrRailBelow());
			return !(state.is(Blocks.POWERED_RAIL) && state.getValue(PoweredRailBlock.POWERED));
		}

		return isPoweredRail;
	}
}
