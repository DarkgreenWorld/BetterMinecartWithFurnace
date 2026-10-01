package betterminecartwithfurnace.mixin;

import betterminecartwithfurnace.ExtinguishMinecartWithFurnaceDispenseBehavior;
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
	/** 原版的水桶发射行为是个匿名类，不方便注入，所以在取行为的地方包一层。 */
	@ModifyReturnValue(method = "getDispenseMethod", at = @At("RETURN"))
	private DispenseItemBehavior betterMinecartWithFurnace$extinguishWithWaterBucket(DispenseItemBehavior original, Level level, ItemStack itemStack) {
		return itemStack.is(Items.WATER_BUCKET) ? new ExtinguishMinecartWithFurnaceDispenseBehavior(original) : original;
	}
}
