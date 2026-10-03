package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
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
	 * This check decides both boosting and braking: for a burning minecart with furnace, an active powered
	 * rail counts as a plain rail. is(T) is declared in TypedInstance, so only one of the targets matches.
	 */
	@ModifyExpressionValue(method = "moveAlongTrack", at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"),
			@At(value = "INVOKE", target = "Lnet/minecraft/core/TypedInstance;is(Ljava/lang/Object;)Z")
	})
	private boolean betterMinecartWithFurnace$ignoreActivePoweredRail(boolean isPoweredRail) {
		if (isPoweredRail && this.minecart instanceof MinecartFurnace furnace && ((MinecartFurnaceAccessor) furnace).betterMinecartWithFurnace$hasFuel()) {
			BlockState state = this.level().getBlockState(this.minecart.getCurrentBlockPosOrRailBelow());
			return !(state.is(Blocks.POWERED_RAIL) && state.getValue(PoweredRailBlock.POWERED));
		}

		return isPoweredRail;
	}
}
