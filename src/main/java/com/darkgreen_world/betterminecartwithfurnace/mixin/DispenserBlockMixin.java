package com.darkgreen_world.betterminecartwithfurnace.mixin;

import com.darkgreen_world.betterminecartwithfurnace.ExtinguishMinecartWithFurnaceDispenseBehavior;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
	/** The vanilla bucket behavior is an anonymous class and awkward to inject into, so wrap it where it is looked up. */
	@ModifyReturnValue(method = "getDispenseMethod", at = @At("RETURN"))
	private DispenseItemBehavior betterMinecartWithFurnace$extinguishWithWaterBucket(DispenseItemBehavior original, Level level, ItemStack itemStack) {
		return itemStack.is(Items.WATER_BUCKET) ? new ExtinguishMinecartWithFurnaceDispenseBehavior(original) : original;
	}
}
