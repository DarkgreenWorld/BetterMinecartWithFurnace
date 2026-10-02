# 更好的动力矿车（Better Furnace Minecart）

Minecraft Java 版 26.3 的 Fabric 模组，**纯服务端**：只装在服务器上即可，原版客户端可以直接进入。不依赖 Fabric API。

## 功能

- **逐渐加速**：点着的动力矿车不再一 tick 就达到最高速度，而是按加速度匀加速。
- **动力铁轨**：
  - 在未充能的动力铁轨上发动机不出力。停止期间燃料依然会消耗；铁轨充能后可加速离开。
  - 充能的动力铁轨不会对**正在燃烧**的动力矿车加速。
  - 熄灭的动力矿车可以用充能的动力铁轨加速。
- **水桶熄灭**：主手拿水桶右键点着的动力矿车，或者用装有水桶的发射器正对矿车并激活，都可以将其熄灭。剩余燃烧时间和行进方向都会保留，水桶不消耗。熄灭后保留动量。
- **漏斗补充燃料**：熄灭的动力矿车正上方若有朝下、未被红石锁住的漏斗时，每 8 tick 从里面取 1 个煤炭/木炭，直到加满为止。漏斗只补充燃料，不会点燃矿车。燃烧中的矿车不会从漏斗取燃料。
- 重新点燃熄灭矿车的方式：
  - 主手拿打火石右键；主手拿煤炭/木炭右键补充燃料；装有打火石的发射器正对矿车并被激活。
  - 点燃后按熄灭前的方向前进；如果没有“之前的方向”，则朝远离玩家/发射器的方向走。
  - 没有燃料时打火石无法点燃动力矿车。

熄灭状态用实体标签 `better_minecart_with_furnace.extinguished` 记录，也可以用命令操作：

```
/tag @e[type=minecart_with_furnace,sort=nearest,limit=1] add better_minecart_with_furnace.extinguished
```

## 配置

启动后生成 `config/better_minecart_with_furnace.properties`：

```properties
thrust=0.6
```
加速度 = 推力 / 15

若您同时安装了Linkart Overhaul，推力将参与决定动力矿车能够拉动矿车组的最高速度

## 构建

需要 JDK 25 和 Gradle 9.5 以上：

```bash
gradle build
```
