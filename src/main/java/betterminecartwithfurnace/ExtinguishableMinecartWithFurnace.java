package betterminecartwithfurnace;

import net.minecraft.world.phys.Vec3;

/** 由 MinecartFurnaceMixin 实现，供其他 mixin（发射器、矿车物理）读取和操作动力矿车的状态。 */
public interface ExtinguishableMinecartWithFurnace {
	/** 有剩余燃烧时间但没点着：被水熄灭的，或者空矿车由漏斗加了燃料的。 */
	boolean betterMinecartWithFurnace$isExtinguished();

	/** 正在燃烧：有燃料且没有被熄灭。 */
	boolean betterMinecartWithFurnace$isBurning();

	/** 熄灭正在燃烧的矿车。没在燃烧（空的或已经熄灭）时什么也不做并返回 false。 */
	boolean betterMinecartWithFurnace$extinguish();

	/**
	 * 点燃熄灭状态的矿车，行进方向沿用熄灭前的；
	 * 没有可沿用的方向时（空矿车由漏斗加的燃料），和原版加燃料一样朝远离 igniterPos 的方向走。
	 */
	void betterMinecartWithFurnace$reignite(Vec3 igniterPos);
}
