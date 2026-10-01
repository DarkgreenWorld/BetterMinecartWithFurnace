package betterminecartwithfurnace;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterMinecartWithFurnace implements ModInitializer {
	public static final String MOD_ID = "better_minecart_with_furnace";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** 被水桶熄灭的动力矿车带有这个实体标签（随原版 Tags 存档，也可以用 /tag 手动增删）。 */
	public static final String EXTINGUISHED_TAG = MOD_ID + ".extinguished";

	/** 充能动力铁轨每 tick 给矿车增加的速度（格/tick），也是本模组加速度的上限。 */
	public static final double POWERED_RAIL_ACCELERATION = 0.06;

	private static final double DEFAULT_ACCELERATION = 0.01;
	private static final double MIN_ACCELERATION = 0.001;
	private static final String ACCELERATION_KEY = "acceleration";

	private static double acceleration = DEFAULT_ACCELERATION;

	/** 动力矿车发动机每 tick 最多增加的速度（格/tick）。 */
	public static double acceleration() {
		return acceleration;
	}

	@Override
	public void onInitialize() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");

		try {
			if (Files.exists(file)) {
				acceleration = readAcceleration(file);
			} else {
				writeDefaults(file);
			}
		} catch (IOException e) {
			LOGGER.warn("无法读写配置文件 {}，使用默认加速度 {}", file, DEFAULT_ACCELERATION, e);
		}
	}

	private static double readAcceleration(Path file) throws IOException {
		Properties properties = new Properties();

		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		String raw = properties.getProperty(ACCELERATION_KEY);

		if (raw == null) {
			return DEFAULT_ACCELERATION;
		}

		double value;

		try {
			value = Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("{} 中的 {}={} 不是数字，使用默认值 {}", file, ACCELERATION_KEY, raw, DEFAULT_ACCELERATION);
			return DEFAULT_ACCELERATION;
		}

		// !(a >= b) 的写法顺便把 NaN 挡在外面
		if (!(value >= MIN_ACCELERATION) || value > POWERED_RAIL_ACCELERATION) {
			double clamped = value > POWERED_RAIL_ACCELERATION ? POWERED_RAIL_ACCELERATION : MIN_ACCELERATION;
			LOGGER.warn("{}={} 超出允许范围 [{}, {}]，改用 {}", ACCELERATION_KEY, raw, MIN_ACCELERATION, POWERED_RAIL_ACCELERATION, clamped);
			return clamped;
		}

		return value;
	}

	private static void writeDefaults(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.write(file, List.of(
				"# 更好的动力矿车 / Better Furnace Minecart",
				"#",
				"# 动力矿车起步时每 tick 增加的速度，单位：格/tick。",
				"# 允许范围 " + MIN_ACCELERATION + " ~ " + POWERED_RAIL_ACCELERATION + "（" + POWERED_RAIL_ACCELERATION + " 即充能动力铁轨的加速度，不能超过它）。",
				"# 动力矿车最高速度为 0.2 格/tick，所以 0.01 表示约 20 tick（1 秒）加速到最高速度。",
				"# 修改后需要重启服务器。",
				ACCELERATION_KEY + "=" + DEFAULT_ACCELERATION
		), StandardCharsets.UTF_8);
	}
}
