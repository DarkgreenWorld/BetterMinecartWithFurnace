package betterminecartwithfurnace.mixin;

import betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.FlintAndSteelDispenseItemBehavior;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

@Mixin(FlintAndSteelDispenseItemBehavior.class)
public abstract class FlintAndSteelDispenseItemBehaviorMixin {
	/**
	 * 原版发射器用打火石时先调用 tryIgniteExplosiveEntities 点燃面前一格里的实体（硫磺方块怪），
	 * 返回 true 就不再放火，并照常扣耐久、播放音效。这里把熄灭状态的动力矿车也算进去。
	 * 空的动力矿车不是熄灭状态，点不着。
	 */
	@ModifyExpressionValue(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/FlintAndSteelDispenseItemBehavior;tryIgniteExplosiveEntities(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z"))
	private boolean betterMinecartWithFurnace$igniteMinecartWithFurnaces(boolean ignited, BlockSource source, ItemStack dispensed) {
		BlockPos targetPos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));

		for (MinecartFurnace minecart : source.level().getEntitiesOfClass(MinecartFurnace.class, new AABB(targetPos))) {
			ExtinguishableMinecartWithFurnace extinguishable = (ExtinguishableMinecartWithFurnace) minecart;

			if (minecart.isAlive() && extinguishable.betterMinecartWithFurnace$isExtinguished()) {
				extinguishable.betterMinecartWithFurnace$reignite(source.center());
				ignited = true;
			}
		}

		return ignited;
	}
}
