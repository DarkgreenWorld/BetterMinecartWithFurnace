package betterminecartwithfurnace.mixin;

import betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
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
	 * moveAlongTrack 开头的 {@code state.is(Blocks.POWERED_RAIL)} 同时决定加速（powerTrack）和刹车（haltTrack）。
	 * 对正在燃烧的动力矿车来说，充能的动力铁轨当作普通铁轨（不加速），未充能的照常刹车。
	 * 空的、熄灭的动力矿车不受影响，仍然可以用动力铁轨发车。
	 * is(T) 是 TypedInstance 的泛型默认方法，擦除后参数是 Object；两个 @At 只会命中其中一个。
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
