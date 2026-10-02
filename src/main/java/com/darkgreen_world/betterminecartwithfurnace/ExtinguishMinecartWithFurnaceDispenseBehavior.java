package com.darkgreen_world.betterminecartwithfurnace;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/** Dispensed water bucket: puts out a minecart with furnace in front instead of placing water. */
public final class ExtinguishMinecartWithFurnaceDispenseBehavior implements DispenseItemBehavior {
	private final DispenseItemBehavior fallback;

	public ExtinguishMinecartWithFurnaceDispenseBehavior(DispenseItemBehavior fallback) {
		this.fallback = fallback;
	}

	@Override
	public ItemStack dispense(BlockSource source, ItemStack dispensed) {
		ServerLevel level = source.level();
		Direction facing = source.state().getValue(DispenserBlock.FACING);
		List<MinecartFurnace> minecarts = level.getEntitiesOfClass(MinecartFurnace.class, new AABB(source.pos().relative(facing)));

		if (minecarts.isEmpty()) {
			return this.fallback.dispense(source, dispensed);
		}

		boolean extinguished = false;

		for (MinecartFurnace minecart : minecarts) {
			if (minecart.isAlive() && ((ExtinguishableMinecartWithFurnace) minecart).betterMinecartWithFurnace$extinguish()) {
				extinguished = true;
			}
		}

		// Same level events as vanilla OptionalDispenseItemBehavior
		level.levelEvent(extinguished ? 1000 : 1001, source.pos(), 0);

		if (extinguished) {
			level.levelEvent(2000, source.pos(), facing.get3DDataValue());
		}

		return dispensed;
	}
}
