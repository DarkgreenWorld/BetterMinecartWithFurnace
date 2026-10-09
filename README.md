# Better Furnace Minecart 更好的动力矿车
[![Modrinth](https://img.shields.io/badge/Published%20on-Modrinth-1bd96a?logo=modrinth&logoColor=bluegreen)](https://modrinth.com/mod/better-minecart-with-furnace)
[![CurseForge](https://img.shields.io/badge/Published%20on-CurseForge-f16436?logo=curseforge&logoColor=orange)](https://www.curseforge.com/minecraft/mc-mods/better-minecart-with-furnace/)
[![GitHub](https://img.shields.io/badge/%E2%80%8B-GitHub-gray?logo=github&logoColor=black&labelColor=white)](https://github.com/DarkgreenWorld/Linkart-Overhaul)

### A Fabric mod for 26.3. Server required, client optional.
#### Requires <img alt="Fabric API icon" src="https://cdn.modrinth.com/data/P7dR8mSH/icon.png" width="20" height="20"> [Fabric API](https://modrinth.com/mod/fabric-api)

## Features

- **Gradual acceleration**: A lit furnace minecart no longer reaches maximum speed in a single tick; instead, it accelerates uniformly according to its acceleration.
- **Powered Rails**: An unpowered powered rail slows down a Minecart with Furnace, just like any other minecart. Fuel is still consumed while stopped; once the rail is powered, the minecart can accelerate away.
- **Water Bucket Extinguishing**: Right-clicking a lit furnace minecart with a water bucket in the main hand, or activating a dispenser loaded with a water bucket facing the minecart, will extinguish it. Remaining burn time and travel direction are preserved, and the water bucket is not consumed. Momentum is retained after extinguishing.
- **Hopper Refueling**: If there is a downward-facing, non-redstone-locked hopper directly above an extinguished furnace minecart, 1 coal/charcoal is taken from it every 8 ticks until the minecart is full. The hopper only refuels the minecart and does not ignite it. A burning minecart will not take fuel from a hopper.
- Ways to relight an extinguished minecart:
  - Right-click with flint and steel in the main hand; right-click with coal/charcoal in the main hand to refuel; a dispenser loaded with flint and steel facing the minecart and activated.
  - After being lit, it travels in the direction it was moving before being extinguished; if there is no "previous direction," it moves away from the player/dispenser.
  - Without fuel, flint and steel cannot ignite a furnace minecart.

The extinguished state is recorded with the entity tag `better_minecart_with_furnace.extinguished`, and can also be manipulated with commands:

```
/tag @e[type=minecart_with_furnace,sort=nearest,limit=1] add better_minecart_with_furnace.extinguished
```

## Configuration

On startup, `config/better_minecart_with_furnace.properties` is generated:

```properties
thrust=1.0
maxAcceleration=0.04
```
See the config file comments for details.

If you also have Linkart Overhaul installed, thrust will participate in determining the maximum speed at which the furnace minecart can pull a train of minecarts.

## Building

Requires JDK 25 and Gradle 9.5 or above:

```bash
gradlew build
```
