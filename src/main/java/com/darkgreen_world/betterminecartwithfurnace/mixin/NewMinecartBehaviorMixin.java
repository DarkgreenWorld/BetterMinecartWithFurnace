package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Same rule under the experimental minecart physics (minecart_improvements): active powered rails do not boost a burning minecart with furnace. */
@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {
	protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
		super(minecart);
	}

	@Inject(method = "calculateBoostTrackSpeed", at = @At("HEAD"), cancellable = true)
	private void betterMinecartWithFurnace$ignoreActivePoweredRail(Vec3 deltaMovement, BlockPos pos, BlockState state, CallbackInfoReturnable<Vec3> cir) {
		if (this.minecart instanceof ExtinguishableMinecartWithFurnace furnace && furnace.betterMinecartWithFurnace$isBurning()) {
			cir.setReturnValue(deltaMovement);
		}
	}
}
