# NFM / NFMZC 单颗粒钠扩散–应力模型

本项目通过 COMSOL Multiphysics 6.4 Java API 在 Windows 上建立、求解并导出二维轴对称正极颗粒模型。正式模型只由 Java 与 properties/CSV 生成；GUI 仅用于检查生成的 `.mph`，任何 GUI 修正都必须回写到 Java 后重新生成。

## 研究范围与物理含义

研究对象为 NFM（NaNi0.2Fe0.4Mn0.4O2）和 NFMZC（NaNi0.185Zn0.015Fe0.4Mn0.37Co0.03O2）半径 2.5 μm 的单个球形颗粒。二维 `r-z` 半圆绕 `r=0` 旋转代表完整球体，能保留球形扩散与轴对称应力，同时远低于三维全电池的计算与参数成本，适合先检查符号、守恒、浓度梯度和化学应变耦合。

第一阶段不包含负极、隔膜、电解液、多孔电极、Butler–Volmer、随机多颗粒或三维全电池。其假设是：球形、均匀表面电流、固相 Fick 扩散、线弹性小变形、准静态力学、各向同性化学本征应变。外圆弧机械自由；轴对称条件消除径向刚体运动，原点的最小点约束消除轴向平移。完全均匀的自由膨胀不会产生显著应力，应力主要来自颗粒内部浓度梯度。

“4.3 V 时脱钠 0.8 mol”在本项目中明确指 `Na1 → Na0.2`，即移除 0.8 mol Na，而不是“剩余 Na 比例为 0.8”。因此 `initial.x=1.0`、`final.x.charge=0.2`，充电时间是 `tCharge=0.8/C_rate[h]`，不是 `1/C_rate[h]`。

## 方程与符号

归一化钠含量为 `xNa=cNa/csmax`，扩散方程为：

`∂cNa/∂t = ∇·(DsEffective ∇cNa)`。

单颗粒量按完整球体计算：`Vp=4πRp³/3`、`mp=rho_p Vp`、`Qp=mp Capacity`、`I_1C=Qp/1[h]`、`I_app=C_rate I_1C`、`Ap=4πRp²`，最终 `NNa=I_app/(F Ap)`，单位为 `mol/(m²·s)`。模型假设全部球面电流均匀分布。系数型 PDE 的自然边界源使用 `signedNaFlux=runSign*NNa`：`chargeSign=-1`，充电使平均浓度下降；`dischargeSign=+1`，放电使平均浓度上升。轴线为零通量。

第一阶段线性化学应变为 `epsilonChem=betaEffective*(cNa-cRef)/csmax`，其中 `cRef=cs0`。它作为三向相等的本征应变加入 Solid Mechanics，不创建真实热传导场。`strain.mode=interpolation` 可改用 CSV；`phase_transition` 预留平滑相变附加应变。NFMZC 的 `gradient.enabled=true` 后可使用 `rNorm=sqrt(r²+z²)/Rp` 对 `Ds` 和 `beta` 做幂律径向插值，不改变几何。

## 参数可信度

配置中的密度、容量、`csmax`、`Ds`、`E`、`nu`、`beta`、相变参数与梯度参数当前均标为 `provisional`。NFMZC 的扩散系数人为设为 NFM 的两倍、`beta` 较低，仅用于机制敏感性和代码趋势检查，不能直接作为论文定量结论。本模型检验的是“在给定参数条件下是否出现较低浓度梯度和应力”，不预设材料一定更优。

如果结论完全来自人为赋予 NFMZC 更优的参数，就属于循环论证。正式研究必须用独立 GITT、原位 XRD、纳米压痕或可靠文献约束 `Ds(x)`、`E`、`nu`、`strain(x)`、相变区间及梯度层参数，并在配置注释中改写来源状态。

## Windows + COMSOL 6.4 编译

默认目录：

```bat
set "COMSOL_HOME=C:\Program Files\COMSOL\COMSOL64\Multiphysics"
set "PROJECT_HOME=D:\work\comsol-na-stress"
scripts\compile_windows.bat
```

PowerShell：

```powershell
$env:COMSOL_HOME='C:\Program Files\COMSOL\COMSOL64\Multiphysics'
$env:PROJECT_HOME='D:\work\comsol-na-stress'
.\scripts\compile_powershell.ps1
```

脚本优先查找 `%COMSOL_HOME%\bin\win64\comsolcompile.exe`，找不到时给出 `COMSOL executable not found. Please set COMSOL_HOME.`。`JAVA_HOME` 可按单位的 COMSOL 安装策略设置；通常优先使用 COMSOL 自带兼容 Java 环境。COMSOL API jar 一般位于安装目录，属于专有组件，不能假设可从公共 Maven 仓库下载。`pom.xml` 用于目录结构、IDE 和不依赖 COMSOL 的基础检查；正式编译推荐 `comsolcompile`。

## Windows 运行

批处理脚本把应用参数放入 `NFM_COMSOL_ARGS`，再用 `comsolbatch` 执行已编译 class，避免 `--material` 被 COMSOL 自己的命令行解析器吞掉：

```bat
scripts\run_windows.bat --build-only --material NFM --c-rate 1 --mode charge
scripts\run_windows.bat --smoke-test
scripts\run_windows.bat --material NFMZC --c-rate 0.5 --mode charge --solve
scripts\run_windows.bat --material NFM --c-rate 1 --mode discharge --solve
scripts\run_windows.bat --all --mode charge
scripts\run_windows.bat --material NFM --c-rate 1 --mode charge --mesh-convergence
scripts\run_windows.bat --material NFM --c-rate 1 --mode charge --export-only
```

PowerShell 使用相同参数：`.\scripts\run_powershell.ps1 --smoke-test`。每次运行先从项目根目录加载配置。`--build-only` 建几何、物理、网格、研究与结果节点并保存 mph，不求解；`--smoke-test` 用 NFM、1C、极短时间和很粗网格验证创建、网格、求解、保存、CSV 与 PNG；`--all` 运行两种材料的 0.1C/0.5C/1C/2C；`--mesh-convergence` 顺序运行 normal/fine/extra_fine 并输出相邻峰值应力变化与 3% 收敛判断。

放电默认先真实求解充电，再把充电末态用作放电初值。COMSOL 6.4 若不接受研究间初值属性，程序会失败并记录异常，不会生成伪放电结果。独立低钠初态尚未实现，选择非 `continue_charge` 会明确抛错。

## 配置和数据

- `config/nfm.properties`、`nfmzc.properties`：材料、相变和梯度参数。
- `config/simulation.properties`：版本、倍率、网格、求解、统一色标和输出。
- `data/ds_*_template.csv`：`x,value`，扩散率单位 `m²/s`。
- `data/strain_*_template.csv`：`x,strain`，应变无量纲。

`diffusion.mode=constant|interpolation`；`strain.mode=linear|interpolation|phase_transition`。选中的 CSV 不存在时配置加载立即失败，不会静默回退。NFM/NFMZC 对比图使用配置中的同一浓度色标（默认 0.2–1.0）和同一应力色标（默认 0–500 MPa）；禁止单图自动缩放后做误导性比较。

## 输出与可重复性

每个工况产生：

- `output/mph/<材料>_<模式>_<倍率>C.mph`
- `output/csv/*_metrics.csv`：末态平均/表面/中心浓度、浓度差、峰值/平均/标准差/主应力及中心/表面径向与环向应力
- `output/csv/*_time_series.csv`：全部固定输出时刻的同组指标
- `output/csv/*_radial_profiles.csv`：20%、50%、80%、100% 等解时刻的 `r/Rp`、`xNa`、`cNa`、von Mises、径向/环向应力和化学应变
- `output/figures/*_concentration_SOC*.png` 与 `*_stress_SOC*.png`
- `output/logs/run_<timestamp>.log` 与工况配置快照目录

日志记录开始/结束时间、实际与预期 COMSOL 版本、Java 版本、材料、倍率、模式、网格、参数文件、输出路径、关键参数、成功/失败状态和异常堆栈。验证会检查单颗粒域、轴线/表面选择、参数范围、NaN/Infinity、充放电浓度方向、初始近零应力、输出目录，以及 `--all` 时低/高倍率浓度梯度关系。

## COMSOL 6.4 GUI 核验清单

用 GUI 打开 Java 生成的 mph 后逐项检查：

1. Component 为 2D Axisymmetric，半圆直边精确位于 `r=0`，旋转后为完整球；半径为 2.5 μm。
2. `particle_domain` 恰有一个域；`axis_boundary`、`surface_boundary`、`fix_point_or_constraint` 非空且没有依赖固定边界号。
3. 外圆弧使用充/放电通量，轴线零通量；充电 `signedNaFlux<0`，末态平均 `xNa` 下降。
4. 初始 `cNa=cs0`；`epsilonChem` 三向相同；初始均匀浓度下峰值应力接近零。
5. 外表面机械自由；只在原点用最小约束消除轴向平移，不把整个表面固定。
6. 表面网格比内部更细，高倍率表面梯度可解析；normal/fine/extra_fine 收敛表合理。
7. Results 中核对 `solid.mises`、`solid.sp1`、`solid.sr`、`solid.sphi` 的 COMSOL 6.4 实际变量名。
8. NFM/NFMZC 同倍率同进度图片使用完全相同色标。

发现问题后回到 Builder/配置修正并重新生成 mph，不能在 GUI 修好后把该文件当正式模型。

## 6.4 API 兼容性核验点

版本敏感标识集中在 `ComsolTagUtils` 及对应 Builder，并带 `VERIFY_WITH_GUI`：半圆 `Circle(angle,rot)`、Coefficient Form PDE 的 `FluxBoundary/ZeroFluxBoundary` 与 `g`、Solid Mechanics 嵌套 `ExternalStrain/e0Voigt`、放电研究 `useinitsol/initmethod/initstudy/solnum`、轴对称应力变量、图片的解层索引。首次生产运行建议在 COMSOL 6.4 GUI 建一个最小等价节点，执行 **File → Save As → Model File for Java**，只用导出文件核对这些 API 名称，然后把确认结果回写到本项目的小型 Builder；不要用导出的长 Java 文件替代本项目。

## 已知限制与升级路线

当前仅均匀等效单颗粒，未做电化学界面动力学、电解液耦合、塑性/断裂、各向异性、真实相变、实验参数反演或三维颗粒形貌。下一步顺序建议是：用实验替换 `Ds(x)`/`strain(x)`；验证并启用平滑相变；启用 NFMZC 径向梯度；加入界面动力学与多孔电极；最后在有必要且参数充分时升级到真实形貌或三维。二维单颗粒应作为后续复杂模型的守恒、符号与网格基准。
