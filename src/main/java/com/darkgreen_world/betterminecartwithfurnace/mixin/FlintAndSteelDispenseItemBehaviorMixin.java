package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.ExtinguishableMinecartWithFurnace;
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
	 * When a dispenser uses flint and steel, vanilla first calls tryIgniteExplosiveEntities to ignite entities in the
	 * block in front (sulfur cubes). If that returns true no fire is placed, and durability and sounds are handled as
	 * usual. Extinguished minecarts with furnace are counted here too.
	 * An empty minecart with furnace is not "extinguished" and cannot be lit.
	 */
	@ModifyExpressionValue(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/FlintAndSteelDispenseItemBehavior;tryIgniteExplosiveEntities(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z"))
	private boolean betterMinecartWithFurnace$igniteMinecartsWithFurnace(boolean ignited, BlockSource source, ItemStack dispensed) {
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
