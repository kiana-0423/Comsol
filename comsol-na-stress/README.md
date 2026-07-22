# NFM / NFMZC 钠电池电化学–力学模型

本项目通过 COMSOL Multiphysics 6.4 Java API 在 Linux 和 macOS 上建立、求解并导出两级模型：

- `particle`：二维轴对称正极单颗粒扩散–应力基准，用于检查守恒、通量符号、初始零应力和网格收敛。
- `full-cell`：三维异质代表性全电池单元，包含显式硬碳颗粒、多孔负极粘结剂、隔膜、多孔正极粘结剂和一个 NFM/NFMZC 正极颗粒；计算电解液/固相电势、颗粒 Na 浓度和正极 von Mises 应力。

正式模型只由 Java 与 properties/CSV 生成；GUI 仅用于检查生成的 `.mph`，任何 GUI 修正都必须回写到 Java 后重新生成。

当前工程全部材料、几何、电化学、网格、求解和验收参数及其来源汇总见 [`PROJECT_PARAMETERS.md`](PROJECT_PARAMETERS.md)。

## 二维单颗粒基准

研究对象为 NFM（NaNi0.2Fe0.4Mn0.4O2）和 NFMZC（NaNi0.185Zn0.015Fe0.4Mn0.37Co0.03O2）半径 2.5 μm 的单个球形颗粒。二维 `r-z` 半圆绕 `r=0` 旋转代表完整球体，能保留球形扩散与轴对称应力，同时远低于三维全电池的计算与参数成本，适合先检查符号、守恒、浓度梯度和化学应变耦合。

该基准不包含负极、隔膜、电解液、多孔电极或 Butler–Volmer。其假设是球形、均匀表面电流、固相 Fick 扩散、线弹性小变形、准静态力学和各向同性化学本征应变。完全均匀的自由膨胀不应产生显著 von Mises 应力。

## 三维异质全电池

`full-cell` 是 `representative-microstructure`（代表性微结构单元），重建需求报告中的三块连续区域和显式颗粒拓扑，不代表实际宏观电极厚度。Battery Module 在负极/正极多孔导电粘结剂和隔膜中求解电解液传输与电势，内部电极表面使用 Butler–Volmer 动力学；局部反应电流通过 `iloc/F` 转换成正负极颗粒表面 Na 摩尔通量。硬碳颗粒只求电化学与固相扩散；Solid Mechanics 仅作用于研究对象——自由正极颗粒，并用刚体运动抑制消除零能模态。正极浓度单向传递为化学本征应变，不加入缺少偏摩尔体积数据的应力反馈扩散项。

默认运行顺序为电流分布初始化、2.7–4.3 V 恒流充电、从充电末态继续到 2.0 V 放电。时间求解器使用电压停止条件，而不是把 `xNa=0.2` 强制当作电压条件。准静态力学方程在保存的瞬态状态上求解且不反馈电化学方程；COMSOL 自动求解器应把电化学/扩散与位移放入顺序 segregated steps，首次6.4实机运行必须按核验清单确认该顺序。

三维几何尺寸、硬碳/电解液/粘结剂参数位于 `config/full_cell.properties`。这些几何值是依据报告拓扑重建并按正负极 Na 容量配平的代表单元，不代表整枚宏观电芯；`negative_capacity_ratio` 必须不小于 1。

“脱钠约0.8 mol”指每摩尔完全钠化化学式由 `Na1` 名义变化到 `Na0.2`，而不是“剩余Na比例为0.8”。`x=0.2`只作为归一化和安全时间窗参考；三维充电由4.3 V停止条件决定真实末态。由于倍率电流按实验容量 `175 mAh/g` 定义，库存参考时间必须用 `Δx·F·csmax·Vp/I_app` 计算，不能直接写成 `0.8/C_rate[h]`。

## 方程与符号

归一化钠含量为 `xNa=cNa/csmax`，扩散方程为：

`∂cNa/∂t = ∇·(DsEffective ∇cNa)`。

单颗粒量按完整球体计算：`Vp=4πRp³/3`、`mp=rho_p Vp`、`Qp=mp Capacity`、`I_1C=Qp/1[h]`、`I_app=C_rate I_1C`、`Ap=4πRp²`，最终 `NNa=I_app/(F Ap)`，单位为 `mol/(m²·s)`。模型假设全部球面电流均匀分布。系数型 PDE 的自然边界源使用 `signedNaFlux=runSign*NNa`：`chargeSign=-1`，充电使平均浓度下降；`dischargeSign=+1`，放电使平均浓度上升。轴线为零通量。

第一阶段线性化学应变为 `epsilonChem=betaEffective*(cNa-cRef)/csmax`，其中 `cRef=cs0`。它作为三向相等的本征应变加入 Solid Mechanics，不创建真实热传导场。`strain.mode=interpolation` 可改用 CSV；`phase_transition` 预留平滑相变附加应变。NFMZC 的 `gradient.enabled=true` 后可使用 `rNorm=sqrt(r²+z²)/Rp` 对 `Ds` 和 `beta` 做幂律径向插值，不改变几何。

## 参数与可信度门槛

正极 GITT 平均扩散系数已按需求文件录入：NFM 充电/放电分别为 `1.08e-13/1.40e-13 m²/s`，NFMZC 为 `1.84e-13/2.76e-13 m²/s`。`csmax` 由密度和化学式摩尔质量计算，倍率使用实验容量 `175 mAh/g` 标定。

OCV、硬碳动力学、化学应变和大部分力学/多孔介质参数仍是明确标记的模板。只有当材料和全电池配置均为 `measured`，且 `strain.mode=interpolation` 使用 XRD 应变曲线时，导出文件中的 `quantitative_ready` 才会为 `true`。在此之前只能报告趋势和敏感性范围，不能把峰值应力作为论文定量结论。

如果结论完全来自人为赋予 NFMZC 更优的参数，就属于循环论证。正式研究必须用独立 GITT、原位 XRD、纳米压痕或可靠文献约束 `Ds(x)`、`E`、`nu`、`strain(x)`、相变区间及梯度层参数，并在配置注释中改写来源状态。

## Linux / macOS + COMSOL 6.4 编译

如果 `comsol` 已在 `PATH` 中，可以直接运行；否则把 `COMSOL_HOME` 指向包含 `bin/comsol` 的 Multiphysics 安装目录：

```sh
export COMSOL_HOME=/usr/local/comsol64/multiphysics   # Linux示例
# export COMSOL_HOME=/Applications/COMSOL64/Multiphysics  # macOS示例
export PROJECT_HOME="$PWD"
chmod +x scripts/*.sh
scripts/audit_inputs.sh
scripts/compile_unix.sh
```

脚本调用COMSOL官方的 `comsol compile`，不依赖系统Maven下载专有API jar。若单位安装在其他目录，只需设置 `COMSOL_HOME`；脚本也会检查常见Linux和macOS安装位置。

## Linux / macOS 运行

运行脚本通过 `NFM_COMSOL_ARGS` 向应用传递参数，再使用 `comsol batch -classpathadd ... -inputfile ...` 执行编译后的主类：

```sh
scripts/run_unix.sh --build-only --material NFM --c-rate 1 --mode charge
scripts/run_unix.sh --smoke-test
scripts/run_unix.sh --material NFMZC --c-rate 0.1 --mode charge --solve
scripts/run_unix.sh --material NFM --c-rate 1 --mode discharge --solve
scripts/run_unix.sh --all --mode charge
scripts/run_unix.sh --material NFM --c-rate 1 --mode charge --mesh-convergence
```

三维全电池：

```sh
scripts/run_unix.sh --model full-cell --build-only --material NFM --c-rate 1 --mode cycle
scripts/run_unix.sh --model full-cell --smoke-test
scripts/run_unix.sh --model full-cell --material NFM --c-rate 0.1 --mode cycle
scripts/run_unix.sh --model full-cell --all --mode cycle
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --mesh-convergence
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --time-convergence
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --parameter-sensitivity
scripts/run_unix.sh --model full-cell --c-rate 1 --mode cycle --parameter-attribution
```

不指定 `--mode` 时，`particle` 默认充电，`full-cell` 默认完整充放电循环。`--all` 对 NFM/NFMZC 执行 0.1C 和 1C。

每次运行先从项目根目录加载配置。`--build-only` 建几何、物理、网格、研究与结果节点并保存 mph，不求解；`--smoke-test` 用 NFM、1C、极短时间和粗网格验证创建、网格、求解、保存、CSV 与 PNG；`--all` 运行两种材料的 0.1C/1C；`--mesh-convergence` 顺序运行 normal/fine/extra_fine，并分别检查平均浓度1%、最大浓度差2%、最大体积平均应力3%和节点采样 `σ95` 5%，单点最大应力只作辅助；`--parameter-sensitivity` 独立扫描正极和硬碳界面动力学；`--parameter-attribution` 在共同NFM基准上分别计算扩散、beta、杨氏模量、正极动力学、合并力学以及全部已配置材料差异的贡献。

放电默认先真实求解充电，再把充电末态用作放电初值。COMSOL 6.4 若不接受研究间初值属性，程序会失败并记录异常，不会生成伪放电结果。独立低钠初态尚未实现，选择非 `continue_charge` 会明确抛错。

## 配置和数据

- `config/nfm.properties`、`nfmzc.properties`：材料、相变和梯度参数。
- `config/simulation.properties`：版本、倍率、网格、求解、统一色标和输出。
- `config/full_cell.properties`：三维几何、硬碳、电解液、多孔介质、截止电压和验收阈值。
- `data/parameter_metadata_*`：逐参数的值、单位、`measured/literature/provisional` 状态、来源与相对不确定度。
- `data/ocv_*`、`k_*`：正负极 OCV 与动力学插值表。
- `data/electrolyte_conductivity_parameters_docx.csv`：从相似项目参数文档数字化并启用的电解液电导率曲线。
- `data/ds_nfm*_template.csv`、`data/ds_nfmzc*_template.csv`：`x,value`，正极扩散率单位 `m²/s`；充/放电分别配置脱钠/嵌钠曲线。
- `data/ds_hard_carbon_parameters_docx.csv`：从相似项目参数文档数字化并启用的硬碳扩散率曲线。
- `data/strain_*_template.csv`：`x,strain`，应变无量纲。

`diffusion.mode=constant|interpolation`；`strain.mode=linear|interpolation|phase_transition`。选中的 CSV 不存在时配置加载立即失败，不会静默回退。NFM/NFMZC 对比图使用配置中的同一浓度色标（默认 0.2–1.0）和同一应力色标（默认 0–1000 MPa）；禁止单图自动缩放后做误导性比较。应力上限与 Kang 等 2026 年论文 Figure 6 的统一色标一致，并覆盖其报道的 NFM 约 597 MPa 峰值；该数值只作外部验证基准，不作为求解输入或强制验收条件。

目标 COMSOL 机器上的固定测试顺序见 [`COMSOL_6_4_TEST_HANDOFF.md`](COMSOL_6_4_TEST_HANDOFF.md)，GUI/API 节点检查见 [`FULL_CELL_GUI_CHECKLIST.md`](FULL_CELL_GUI_CHECKLIST.md)。本工程不提供 Windows 运行入口。

## 输出与可重复性

每个工况产生：

- `output/mph/<材料>_<模式>_<倍率>C.mph`
- `output/csv/*_metrics.csv`：末态平均/表面/中心浓度、浓度差、峰值/平均/标准差/主应力及中心/表面径向与环向应力
- `output/csv/*_time_series.csv`：全部固定输出时刻的同组指标
- `output/csv/*_radial_profiles.csv`：20%、50%、80%、100% 等解时刻的 `r/Rp`、`xNa`、`cNa`、von Mises、径向/环向应力和化学应变
- `output/figures/*_concentration_SOC*.png` 与 `*_stress_SOC*.png`
- `output/logs/run_<timestamp>.log` 与工况配置快照目录

三维全电池还会产生 `FULLCELL_*` 文件：电压/容量/浓度/应力时间序列、正极径向剖面、目标电压与实际求解层的索引、验收结果、网格/时间步收敛表、敏感性/归因表、峰值应力不确定区间及统一色标 PNG。图像既按指定电压选择最近求解层，也按0/25/50/75/100%名义脱钠进度选择最接近的平均 `xNa`，分别记录在 `*_snapshot_index.csv` 与 `*_soc_snapshot_index.csv`。因此NFM/NFMZC可以同时进行等工作电压和等平均钠含量比较。

0.1C 充电工况会把模拟电压按容量线性插值到 `experimental.curve.csv` 的实验容量点，计算电压 RMSE 和 4.3 V 截止容量相对误差。参数仍为 provisional 时100 mV作为初步警戒线、50 mV作为论文目标；全部关键数据达到 measured 且启用XRD应变曲线后，50 mV才成为正式验收线。1C与放电工况在没有对应实验曲线时将该检查标为不适用，不会用0.1C曲线伪验证。

日志记录开始/结束时间、实际与预期 COMSOL 版本、Java 版本、材料、倍率、模式、网格、参数文件、输出路径、关键参数、成功/失败状态和异常堆栈。验证会检查单颗粒域、轴线/表面选择、参数范围、NaN/Infinity、球形恒通量解析平均浓度（库存误差小于 1%）、充放电浓度方向、初始近零应力、输出目录，以及 `--all` 时低/高倍率浓度梯度关系。

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

三维模型还必须核对 `LithiumIonBattery`、`PorousConductiveBinder`、`InternalElectrodeSurface/er1`、`ElectrodeCurrent/IsTotal`、`liion.ies_*.er1.iloc`、`Image3D` 和 `StopCondition`。核验步骤见 `FULL_CELL_GUI_CHECKLIST.md`。由于仓库环境没有 COMSOL 6.4，这些节点尚未在本机执行验证。

## 已知限制与升级路线

当前三维模型仍不包含塑性、断裂、晶体各向异性、真实相变或应力反馈扩散。NFMZC 主计算保持均质有效材料；只有取得径向 EDS/相分布和参数映射后才启用显式梯度。二维单颗粒继续作为三维模型的守恒、符号与网格基准。

## Kang 2026 文献约束的使用边界

Kang 等（Advanced Functional Materials, 2026, DOI: 10.1002/adfm.202531556）的原始 NFM 与本项目 NFM 化学式相同，因此可用来校核 NFM：2.0–4.3 V 测试窗口、约 3.3 V 的 O3→P3 起始、4.25–4.30 V 的高压相区、`Δa=1.83%`、`Δc=3.04%`、平均 `DNa=1.17e-12 m²/s` 的量级，以及约 597 MPa 的有限元峰值应力。晶格变化是各向异性边界，并不等于当前各向同性 `chemical.expansion.beta`；没有电压/SOC 全曲线前不启用定量相变应变。

论文中的 NFMC 是 3 wt% Co 表面处理并形成 P2/O3 重构层和 NaxCoO2 包覆的材料，而本项目 NFMZC 是 Zn/Co 体相掺杂。因此 NFMC 的相比例、扩散提升、晶格变化、循环后阻抗和应力降低均不迁移到 NFMZC。逐项判定保存在 `data/kang_2026_reference_constraints.csv`。
