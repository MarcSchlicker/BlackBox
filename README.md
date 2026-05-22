# WIP - Performance-Optimized Farm Dimensions

This project is WIP, feel free to join!
This Minecraft mod aims to drastically reduce lag caused by automated farms by simulating them in separate dimensions with pre-calculated outputs.

## 🌱 About the Mod

Instead of having large redstone or mob farms constantly running in the overworld, this mod allows you to build farms in a custom dimension. Once built, the mod observes the farm's output for a certain amount of time (e.g., 1 hour), then "freezes" that result into a reusable item — the **Dimension Core**.

You can then insert this core into a special **Blackbox** in the overworld or any other dimension to simulate the farm's production without actually running it.

### ✅ Key Features

- **Dimension Core System**  
  Build your farm in a special dimension, observe production, and store it inside an item.

- **No Real-Time Processing**  
  Farms are only calculated once. After that, output is simulated, improving performance drastically.

- **Input/Output Integration**  
  Define how items flow in and out of the simulated system using input/output blocks.

- **Clone & Manage Farms**  
  Advanced crafting and processing options allow you to clone farms or convert them into passive output blocks.

- **Compact & Modular Design**  
  Build once — use many times. Share or duplicate efficient farm setups easily.

## 🔧 Built With

- [MCreator](https://mcreator.net/) — Modding tool used to build and test the mod  
- Minecraft 1.20.x (or your target version)

## 🚀 Installation

_Coming soon…_

## 📌 Current Limitations

- Farms must be built manually in the special dimension.  
  _(This is intentional — the idea is to build once and simulate later. However, in the future, the system may support creating a simulation without requiring a separate dimension.)_

- The new custom dimension is not finished yet.  
  _(Expect changes, placeholder blocks, and incomplete visuals.)_

- No dynamic ticking — all calculations are snapshot-based.  
  _(This is by design to reduce lag and improve performance.)_

- Slot and stack size limitations depend on MCreator capabilities.  
  _(Advanced inventory mechanics like stacking beyond 64 may not be fully supported.)_

## 🧠 Planned Features

- Energy-based automation system and compatibility with external mod energy systems  
  _(To allow more diverse farm types and energy balancing based on the mods you use.)_

- Better GUI for managing simulations  
  _(With improved control over item flow, core settings, and simulation overview.)_

- More dimension presets (e.g., biome-specific variants)  
  _(Simulations may behave differently depending on environmental conditions.)_

- Improved item routing and sorting  
  _(Including automated transport between simulation cores and storage systems.)_

- Optional non-dimensional simulation  
  _(Let farms be simulated "remotely" without building in a dedicated dimension.)_


## 🤝 Contributing

PRs and suggestions are welcome! Please open an issue if you have questions or find bugs.

## 📜 License

MIT License — feel free to modify and use it as you like (please credit if redistributed).

---

> Created with ❤️ using MCreator
