package betterminecartwithfurnace;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/**
 * 发射器里的水桶：面前一格有动力矿车时熄灭它（和玩家手持水桶一样不消耗水），不倒水；
 * 没有动力矿车时交还给原版行为。
 */
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

		// 与原版 OptionalDispenseItemBehavior 相同的反馈：成功是“咔哒”加烟雾，失败是空发射的声音。
		level.levelEvent(extinguished ? 1000 : 1001, source.pos(), 0);

		if (extinguished) {
			level.levelEvent(2000, source.pos(), facing.get3DDataValue());
		}

		return dispensed;
	}
}
