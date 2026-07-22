# NFM / NFMZC COMSOL 项目参数总表

更新日期：2026-07-23  
适用工程：`comsol-na-stress`  
目标环境：COMSOL Multiphysics 6.4，Linux/macOS

## 1. 状态与来源说明

| 标记 | 含义 |
|---|---|
| `supplied` | 用户需求文件、实验信息或用户确认的数据 |
| `legacy-reference` | 相似项目 `parameters.docx` 中的参数；可用于当前试算，但不是本项目实测值 |
| `literature` | Kang et al., Advanced Functional Materials (2026), DOI `10.1002/adfm.202531556` |
| `derived` | 由化学式、几何或其他输入通过明确公式计算 |
| `provisional` | 为模型运行或敏感性分析设置的暂定值，等待实测/可靠文献替换 |
| `numerical-control` | 网格、求解器、导出或验收控制参数，不是材料物性 |
| `inactive` | 已保留但当前主模型不启用的参数 |

当前 NFM、NFMZC 和全电池聚合状态均为 `provisional`，因此 `quantitativeReady=false`。现阶段应力结果只能用于趋势和敏感性比较。

## 2. 正极材料参数

### 2.1 当前主模型参数

| 参数 | NFM | NFMZC | 状态 | 来源/说明 |
|---|---:|---:|---|---|
| 材料名称 | NFM | NFMZC | `supplied` | 用户需求 |
| 化学式 | NaNi₀.₂Fe₀.₄Mn₀.₄O₂ | NaNi₀.₁₈₅Zn₀.₀₁₅Fe₀.₄Mn₀.₃₇Co₀.₀₃O₂ | `supplied` | 用户需求文件 |
| 颗粒半径 `Rp` | 2.5 μm | 2.5 μm | `supplied` | 用户需求；与相似项目大颗粒半径一致 |
| 密度 `rho_p` | 4550 kg/m³ | 4550 kg/m³ | `legacy-reference` | `parameters.docx`；NFMZC 暂沿用 NFM 值 |
| 实验容量 `Capacity` | 175 mAh/g | 175 mAh/g | `supplied` | 用户给定实验容量，用于倍率电流标定 |
| 摩尔质量 `M_formula` | 111.040 g/mol | 111.274 g/mol | `derived` | 由化学式计算 |
| 最大 Na 浓度 `csmax` | 40.976 kmol/m³ | 40.890 kmol/m³ | `derived` | `rho_p/M_formula`；不再使用旧项目统一的 46 kmol/m³ |
| 初始 Na 分数 `xInitial` | 1.0 | 1.0 | `supplied` | 充电定义为 Na₁→Na₀.₂ |
| 名义充电末态 `xFinalCharge` | 0.2 | 0.2 | `supplied` | 脱出约 0.8 mol Na；实际终点由4.3 V截止 |
| 充电扩散系数 | 1.08×10⁻¹³ m²/s | 1.84×10⁻¹³ m²/s | `supplied` | 项目样品 GITT 平均值，脱钠方向 |
| 放电扩散系数 | 1.40×10⁻¹³ m²/s | 2.76×10⁻¹³ m²/s | `supplied` | 项目样品 GITT 平均值，嵌钠方向 |
| 杨氏模量 `E` | 150 GPa | 165 GPa | `provisional` | NFM沿用旧模型；NFMZC采用用户建议的约10%刚化假设 |
| 杨氏模量扫描 | 100、150、200 GPa | 120、165、210 GPa | `provisional` | 用户建议的趋势敏感性范围 |
| 泊松比 `ν` | 0.30 | 0.30 | `provisional` | 用户提供的 O3 层状氧化物各向同性建模建议；两材料取相同基准 |
| 泊松比敏感性值 | 0.25、0.30、0.35 | 0.25、0.30、0.35 | `provisional` | 用户建议；代码按绝对值运行，不再使用总不确定度比例外推 |
| 各向同性化学膨胀系数 `beta` | 0.012 | 0.008 | `provisional` | 用户建议的趋势基准；不是 XRD 实测应变 |
| `beta` 扫描 | 0.008、0.012、0.020 | 0.005、0.008、0.014 | `provisional` | 用户建议范围 |
| Na₁→Na₀.₂等效应变 | −0.96% | −0.64% | `derived` | `beta×(0.2−1)` |
| 正极交换电流密度 `i0_pos` | 5 A/m² | 8 A/m² | `provisional` | 用户建议的倍率曲线校准初值 |
| `i0_pos` 扫描 | 1、5、20 A/m² | 1、8、30 A/m² | `provisional` | 用户建议范围；不能由GITT直接换算 |
| 总相对不确定度 | 25% | 25% | `provisional` | 材料敏感性扫描默认范围 |

配置来源：[nfm.properties](config/nfm.properties)、[nfmzc.properties](config/nfmzc.properties)。

### 2.2 正极 OCV、扩散与应变表

| 数据 | 当前模式/数值 | 状态 | 来源 |
|---|---|---|---|
| NFM OCV | `(x,V)=(0.2,4.30),(0.3,4.10),(0.4,3.90),(0.5,3.70),(0.6,3.52),(0.7,3.35),(0.8,3.18),(0.9,3.05),(1.0,2.95)` | `provisional` | bring-up 曲线；不是原始 GITT OCV |
| NFMZC OCV | `(0.2,4.30),(0.3,4.12),(0.4,3.94),(0.5,3.75),(0.6,3.56),(0.7,3.38),(0.8,3.20),(0.9,3.07),(1.0,2.98)` | `provisional` | bring-up 曲线；不是原始 GITT OCV |
| NFM 扩散表 | 充电全范围 1.08×10⁻¹³；放电全范围 1.40×10⁻¹³ m²/s | `supplied` | 项目 GITT 平均值铺平为常数表 |
| NFMZC 扩散表 | 充电全范围 1.84×10⁻¹³；放电全范围 2.76×10⁻¹³ m²/s | `supplied` | 项目 GITT 平均值铺平为常数表 |
| NFM 应变表 | `strain=0.012(x-1)` | `provisional` | 与当前线性基准同步；仍需XRD数据替换 |
| NFMZC 应变表 | `strain=0.008(x-1)` | `provisional` | 线性占位，从 x=1 的0到 x=0 的−0.008 |
| 当前扩散模式 | `constant` | `numerical-control` | 主模型直接使用方向相关常数；插值模式预留 |
| 当前应变模式 | `linear` | `numerical-control` | 尚未启用 XRD 插值曲线 |

对应文件：[NFM OCV](data/ocv_nfm_template.csv)、[NFMZC OCV](data/ocv_nfmzc_template.csv)、[NFM 应变](data/strain_nfm_template.csv)、[NFMZC 应变](data/strain_nfmzc_template.csv)。

### 2.3 相变与径向梯度预留参数（当前不启用）

| 参数 | NFM | NFMZC | 状态 | 来源 |
|---|---:|---:|---|---|
| `phase.transition.enabled` | false | false | `inactive` | 未取得电压/SOC 分辨 XRD 前关闭 |
| `phase.x.high` | 0.70 | 0.68 | `provisional` | 充电时较高Na含量的相变起始边界 |
| `phase.x.low` | 0.35 | 0.32 | `provisional` | 充电时较低Na含量的相变结束边界 |
| `phase.extra.strain` | 0.006 | 0.003 | `provisional` | 相变附加应变敏感性假设 |
| `phase.smoothing.width` | 0.03 | 0.05 | `provisional` | 相变进度数值平滑尺度 |
| `gradient.enabled` | false | false | `inactive` | NFMZC 主模型采用均质有效材料 |
| `gradient.exponent` | 2 | 2 | `provisional` | 径向幂律敏感性参数 |
| `gradient.diffusion.core` | 1.08×10⁻¹³ | 1.84×10⁻¹³ m²/s | `provisional` | 当前设为各自充电 GITT 值 |
| `gradient.diffusion.surface` | 1.08×10⁻¹³ | 1.84×10⁻¹³ m²/s | `provisional` | 当前无梯度 |
| `gradient.beta.core` | 0.012 | 0.008 | `provisional` | 当前设为均质 beta |
| `gradient.beta.surface` | 0.012 | 0.008 | `provisional` | 当前无梯度 |

## 3. 三维代表单元几何

| 参数 | 当前值 | 状态 | 来源/说明 |
|---|---:|---|---|
| 负极区域长度 | 9 μm | `provisional` | 根据旧报告拓扑重建的代表性尺寸 |
| 几何分类 | `representative-microstructure` | `numerical-control` | 明确为局域代表微结构，不是实际电极厚度 |
| 隔膜长度 | 3 μm | `provisional` | 根据旧报告拓扑重建 |
| 正极区域长度 | 6 μm | `provisional` | 根据旧报告拓扑重建 |
| 总长度 | 18 μm | `derived` | 9+3+6 μm |
| 单元宽度 | 9 μm | `provisional` | 代表单元设计 |
| 单元高度 | 9 μm | `provisional` | 代表单元设计 |
| 横截面积 `A_cell` | 81 μm² | `derived` | `width×height` |
| 正极颗粒半径 | 2.5 μm | `supplied` | 用户需求 |
| 负极大颗粒半径 | 2.3 μm | `provisional` | 旧报告几何图的代表性重建 |
| 负极小颗粒半径 | 1.8 μm | `provisional` | 旧报告几何图的代表性重建 |
| 负极颗粒组合 | 2个大颗粒+2个小颗粒 | `provisional` | Java 几何/容量配平设计 |
| 正极颗粒中心 | `x=L_an+L_sep+L_ca/2`; `y=W/2`; `z=H/2` | `derived` | 几何居中公式 |

配置来源：[full_cell.properties](config/full_cell.properties)。

## 4. 电解液与界面反应参数

| 参数 | 当前值 | 状态 | 来源/说明 |
|---|---:|---|---|
| 初始电解液浓度 `cl0` | 1 M = 1000 mol/m³ | `legacy-reference` | `parameters.docx` |
| 1 M 电导率 | 0.882 S/m | `legacy-reference` | 从旧项目 `sigmal(c)` 图数字化；8.82 mS/cm |
| 电解液扩散系数 `Dl` | 1.5×10⁻¹⁰ m²/s | `provisional` | 用户建议基准；建议扫描0.8–3×10⁻¹⁰并核对COMSOL定义 |
| Na⁺迁移数 `tplus` | 0.40 | `provisional` | 用户建议基准；建议扫描0.30–0.50 |
| 电荷转移系数 `alpha` | 0.5 | `legacy-reference` | `parameters.docx` 的对称反应假设 |
| 电解液电导率曲线 | `(c kmol/m³,κ S/m)=(0.15,0.405),(0.50,0.720),(1.00,0.882),(1.50,0.860),(2.00,0.760)` | `legacy-reference` | 从 `parameters.docx` 图数字化，当前实际加载 |

当前没有采用旧项目的 `cl_ref=1 mol/m³`，因为它与 `cl0=1000 mol/m³` 相差1000倍且归一化定义不清。

## 5. 硬碳负极参数

| 参数 | 当前值 | 状态 | 来源/说明 |
|---|---:|---|---|
| 最大 Na 浓度 `csmax_neg` | 14.54 kmol/m³ | `legacy-reference` | `parameters.docx`；容量/密度推导依据未给出 |
| 初始 Na 分数 `x_neg_initial` | 0.007 | `legacy-reference` | `parameters.docx` |
| 初始浓度 `cs0_neg` | 0.10178 kmol/m³ | `derived` | `csmax_neg×x_neg_initial` |
| 扩散系数回退值 | 1×10⁻¹⁵ m²/s | `legacy-reference` | 仅作回退；实际使用插值表 |
| 杨氏模量 | 15 GPa | `provisional` | 用户建议基准；扫描8–30 GPa |
| 泊松比 | 0.25 | `provisional` | bring-up 假设 |
| 化学膨胀系数 | 0.025 | `provisional` + `inactive` | 用户建议；当前正极单独力学域不使用 |
| OCV | `(0.007,0.250),(0.030,0.215),(0.060,0.175),(0.100,0.140),(0.180,0.105),(0.300,0.082),(0.450,0.065),(0.600,0.052),(0.750,0.041),(0.850,0.032),(0.930,0.025),(1.000,0.020)` | `provisional` | 用户建议的加密 bring-up 曲线 |
| 交换电流密度 | 全 x 范围 3 A/m² | `provisional` | 用户建议校准初值；独立扫描0.5、3、15 A/m² |

### 硬碳扩散系数 `DHC(x)`（当前实际加载）

来源：从相似项目 `parameters.docx` 的 `Ds_n(c)` 图数字化，并用 `x=c/14.54 kmol/m³` 归一化。相对不确定度暂设35%。

| x | D (m²/s) | x | D (m²/s) |
|---:|---:|---:|---:|
| 0.000 | 2.7×10⁻¹⁶ | 0.550 | 1.82×10⁻¹⁵ |
| 0.034 | 3.5×10⁻¹⁶ | 0.619 | 1.45×10⁻¹⁵ |
| 0.069 | 4.1×10⁻¹⁶ | 0.688 | 1.05×10⁻¹⁵ |
| 0.103 | 4.5×10⁻¹⁶ | 0.756 | 5.4×10⁻¹⁶ |
| 0.138 | 4.8×10⁻¹⁶ | 0.825 | 3.1×10⁻¹⁶ |
| 0.172 | 5.2×10⁻¹⁶ | 0.894 | 3.5×10⁻¹⁶ |
| 0.206 | 6.2×10⁻¹⁶ | 0.963 | 7.7×10⁻¹⁶ |
| 0.275 | 9.3×10⁻¹⁶ | 1.000 | 1.27×10⁻¹⁵ |
| 0.344 | 1.35×10⁻¹⁵ |  |  |
| 0.413 | 1.79×10⁻¹⁵ |  |  |
| 0.481 | 1.98×10⁻¹⁵ |  |  |

数据文件：[ds_hard_carbon_parameters_docx.csv](data/ds_hard_carbon_parameters_docx.csv)。

## 6. 多孔介质、粘结剂与隔膜

| 参数 | 当前值 | 状态 | 来源 |
|---|---:|---|---|
| 粘结剂电子电导率 | 10 S/m | `legacy-reference` | `parameters.docx` |
| 粘结剂杨氏模量 | 0.3 GPa | `provisional` | 用户建议的多孔PVDF/导电剂等效网络基准 |
| 粘结剂泊松比 | 0.32 | `provisional` | 用户建议基准 |
| 隔膜杨氏模量 | 0.35 GPa | `provisional` | 用户建议的聚烯烃等效基准；扫描0.20–0.50 GPa |
| 隔膜泊松比 | 0.30 | `provisional` | 用户建议的各向同性基准；扫描0.20–0.40 |
| 负极孔隙率 | 0.35 | `provisional` | bring-up 假设 |
| 正极孔隙率 | 0.30 | `provisional` | 用户建议的代表性多孔电极基准 |
| 隔膜孔隙率 | 0.45 | `provisional` | bring-up 假设 |
| 负极曲折率 | 1.69 | `provisional` | Bruggeman `τ=ε^-1/2`，ε=0.35 |
| 正极曲折率 | 1.83 | `provisional` | Bruggeman `τ=ε^-1/2`，ε=0.30 |
| 隔膜曲折率 | 1.49 | `provisional` | Bruggeman `τ=ε^-1/2`，ε=0.45 |

当前三维 Solid Mechanics 仅选择正极颗粒；粘结剂、隔膜和负极力学参数暂不会改变主模型正极应力结果，但保留用于后续扩展。

当前代码把孔隙率和曲折率分别传给 Battery Module 的 `epsilonl` 与 `taul`，并按预期的几何曲折率形式 `D_eff=εD/τ` 配置上表 `τ=ε^-1/2`。首次 COMSOL 6.4 GUI 核验必须确认该接口的实际定义；若接口采用 `D_eff=ε^bD`，则应改用 Bruggeman 指数 `b=1.5`，不能同时重复施加曲折率修正。

## 7. 充放电工况与停止条件

| 参数 | 当前值 | 状态 | 来源 |
|---|---|---|---|
| 倍率 | 0.1C、1C | `supplied` | 用户计算需求 |
| 默认模式 | charge | `numerical-control` | CLI 默认；全电池批处理通常使用 cycle |
| 放电初始化 | 继承充电末态 | `numerical-control` | 计算计划要求 |
| 充电截止电压 | 4.3 V | `supplied` | 用户需求/实验窗口 |
| 放电截止电压 | 2.0 V | `supplied` | 用户需求/实验窗口 |
| 充电快照电压 | 2.7、3.3、3.7、4.0、4.3 V | `supplied` + `literature` | 用户计划；与 Kang 2026 Figure 6 一致 |
| 放电快照电压 | 4.3、3.5、3.3、3.0、2.6 V | `supplied` + `literature` | 用户计划；与 Kang 2026 Figure 6 一致 |
| SOC 快照比例 | 0、0.25、0.50、0.75、1.0 | `numerical-control` | 等进度比较设计 |
| `runDirection` | 充电 +1；放电 −1 | `numerical-control` | 全电池电流方向约定 |
| 名义时间安全系数 | 1.2 | `numerical-control` | `t_nominal=1.2×max(1h/C,t_to_nominal_x)` |

## 8. 单颗粒与全电池派生参数

| 参数 | NFM | NFMZC | 状态/公式 |
|---|---:|---:|---|
| 正极球体积 `Vp_pos` | 6.54498×10⁻¹⁷ m³ | 同左 | `derived`; `4πRp³/3` |
| 正极球表面积 | 7.85398×10⁻¹¹ m² | 同左 | `derived`; `4πRp²` |
| 正极单颗粒质量 | 2.97797×10⁻¹³ kg | 同左 | `derived`; `rho_p×Vp` |
| 175 mAh/g 对应单颗粒容量 | 1.87612×10⁻⁷ C | 同左 | `derived`; `mp×Capacity` |
| 单颗粒 1C 电流 | 5.21144×10⁻¹¹ A | 同左 | `derived`; `Q/1h` |
| 1C 正极平均摩尔通量 | 6.87712×10⁻⁶ mol/(m²·s) | 同左 | `derived`; `I/(F×4πRp²)` |
| 0.1C 正极平均摩尔通量 | 6.87712×10⁻⁷ mol/(m²·s) | 同左 | `derived` |
| 负极颗粒总体积 | 1.50788×10⁻¹⁶ m³ | 同左 | `derived`; 2大球+2小球 |
| 负/正可用容量比 | 1.0147 | 1.0169 | `derived`; 当前几何和浓度参数 |
| 名义 Na₁→Na₀.₂ 时间（1C电流） | 1.1034 h | 1.1011 h | `derived`; 库存一致公式，不是截止条件 |
| 法拉第常数 `F_const` | 96485.33212 C/mol | 同左 | `derived/physical-constant`; 代码常数 |

由于容量比只略高于1，负极颗粒几何、硬碳 `csmax` 或初始 SOC 的小变化可能导致 `negativeCapacityRatio<1`，需在生产运行前重点复核。

## 9. 网格参数

### 9.1 二维单颗粒

| 参数 | 当前值 | 状态/来源 |
|---|---:|---|
| 网格等级 | normal | `numerical-control` |
| 最大单元 | 0.0833 μm | `numerical-control`; 约 Rp/30 |
| 最小单元 | 0.0125 μm | `numerical-control` |
| 最大增长率 | 1.25 | `numerical-control` |
| 粗网格 `hmax` | 基准×2 | `numerical-control` |
| fine `hmax` | 基准×0.67 | `numerical-control` |
| extra_fine `hmax` | 基准×0.5 | `numerical-control` |
| 表面最大单元 | 体网格 `hmax×0.5` | `derived` |
| smoke 网格 | 0.5 μm | `numerical-control` |

### 9.2 三维全电池

| 参数 | 当前值 | 状态/来源 |
|---|---:|---|
| 全局最大单元 | 0.4 μm | `numerical-control` |
| 最小单元 | 0.05 μm | `numerical-control` |
| 正极颗粒最大单元 | 全局 `hmax×0.5` | `derived` |
| 正极界面最大单元 | 全局 `hmax×0.35` | `derived` |
| 粗/fine/extra_fine 系数 | 2 / 0.67 / 0.5 | `numerical-control` |
| smoke 全局/颗粒/界面最大单元 | 1.5 / 0.75 / 0.5 μm | `numerical-control` |
| smoke 最小单元 | 全局0.2 μm；颗粒0.15 μm | `numerical-control` |

## 10. 求解器、导出和图形参数

| 参数 | 当前值 | 状态/来源 |
|---|---:|---|
| COMSOL 版本 | 6.4 | `supplied`; 当前 Linux/macOS 环境要求 |
| 相对容差 | 1×10⁻⁴ | `numerical-control` |
| 最大时间步比例 | 0.02×名义过程时间 | `numerical-control` |
| 时间步收敛扫描 | 0.04、0.02、0.01 | `numerical-control` |
| 二维输出进度点 | 0、0.2、0.4、0.5、0.6、0.8、1.0 | `numerical-control` |
| 统一导出进度点 | 0.2、0.5、0.8、1.0 | `numerical-control` |
| smoke 求解时长 | 2 s | `numerical-control` |
| 输出根目录 | `output` | `numerical-control` |
| 浓度图统一色标 | xNa 0.2–1.0 | `numerical-control`; 与研究 SOC 范围一致 |
| 应力图统一色标 | 0–1000 MPa | `literature` + `numerical-control`; Kang 2026 Figure 6 |

配置来源：[simulation.properties](config/simulation.properties)。

## 11. 验证与收敛阈值

| 检查项 | 阈值 | 状态/来源 |
|---|---:|---|
| 初始应力 | <1 kPa | `numerical-control`; 初始均匀自由颗粒应近零 |
| 网格平均浓度变化 | <1% | `supplied`; 项目验收计划 |
| 网格最大浓度差变化 | <2% | `numerical-control`; 稳健性补充指标 |
| 网格最大体积平均应力变化 | <3% | `supplied`; 项目验收计划 |
| 网格应力 P95 变化 | <5% | `numerical-control`; 避免单节点奇异峰值主导 |
| 总 Na 物料平衡误差 | <1% | `supplied`; 项目验收计划 |
| 0.1C 电压 RMSE | <50 mV | `supplied`; 定量目标 |
| provisional 电压 RMSE | <100 mV | `numerical-control`; 参数未齐时的调试门槛 |
| 截止容量相对误差 | <5% | `supplied`; 项目验收计划 |
| 浓度单调性数值容差 | 1×10⁻⁷ | `numerical-control`; 导出校验代码 |

## 12. 实验曲线模板

以下数据当前用于程序联调和0.1C曲线校验框架，仍不是高分辨率原始实验数据。

| 材料 | `(容量 mAh/g, 电压 V)` | 状态 | 来源 |
|---|---|---|---|
| NFM | `(0,3.05),(25,3.10),(50,3.23),(75,3.43),(100,3.68),(125,3.98),(150,4.30)` | `provisional` | 用户提供充放电图的近似数字化/bring-up |
| NFMZC | `(0,2.98),(25,3.05),(50,3.18),(75,3.40),(100,3.68),(125,3.95),(150,4.15),(175,4.30)` | `provisional` | 用户提供充放电图的近似数字化/bring-up |

对应文件：[NFM 实验曲线](data/experimental_charge_nfm_template.csv)、[NFMZC 实验曲线](data/experimental_charge_nfmzc_template.csv)。

## 13. Kang 2026 文献约束（仅校核，不作为当前材料输入）

| 文献参数 | 数值 | 用途 |
|---|---:|---|
| 电压窗口 | 2.0–4.3 V | 工况一致性校核 |
| NFM O3→P3 起始 | 约3.3 V | 相变响应校核 |
| NFM 高压 O3′ 区域 | 4.25–4.30 V | 高压相区校核 |
| NFM 晶格 `a` 变化 | 1.83% | 各向异性应变敏感性边界 |
| NFM 晶格 `c` 变化 | 3.04% | 各向异性应变敏感性边界 |
| NFM 平均 GITT `DNa` | 1.17×10⁻¹² m²/s | 量级比较，不覆盖项目 GITT |
| NFM 报道峰值 von Mises 应力 | 597 MPa | 外部结果范围校核，不设为强制结果 |
| NFMC 平均 GITT `DNa` | 2.06×10⁻¹² m²/s | 不迁移至体相掺杂 NFMZC |

完整判定见 [kang_2026_reference_constraints.csv](data/kang_2026_reference_constraints.csv)。论文中的 NFMC 是表面 Co 处理材料，不等于本项目的 Zn/Co 体相掺杂 NFMZC。

## 14. 当前仍需替换的参数

下列参数已有 provisional 值，模型能够引用，但尚未达到论文定量条件：

1. NFM、NFMZC 的原始充/放电 OCV–SOC 表；
2. NFM、NFMZC 的 XRD `a(V/SOC)`、`c(V/SOC)` 和化学应变曲线；
3. 两种正极各自的密度、杨氏模量和精确成分泊松比/单晶弹性矩阵；当前各向同性泊松比基准为0.30；
4. 正极及硬碳的交换电流密度或可换算的完整 Butler–Volmer 动力学数据；
5. 实际电解液的 `Dl(c)`、`t+(c)`、热力学因子和温度；
6. 实际正负极/隔膜厚度、孔隙率、曲折率、面载量、压实密度和 N/P；
7. 硬碳容量、密度、`csmax` 及本项目样品的 `DHC(x)`；
8. 0.1C、1C 充放电原始 CSV，而不是图片数字化曲线；
9. 若启用梯度 NFMZC，需要径向 EDS/相分布和成分—物性映射。

推荐校准顺序固定为：先用0.1C/GITT静置数据校准 OCV；再用0.1C与1C的电压差校准正负极交换电流密度；最后调整电解液 `Dl`、`tplus` 和电导率。不要用同一条倍率曲线同时自由拟合所有参数。

## 15. 权威文件优先级

如本文件与代码不一致，以以下运行输入为准：

1. `config/nfm.properties`、`config/nfmzc.properties`；
2. `config/full_cell.properties`；
3. `config/simulation.properties`；
4. 配置中实际引用的 `data/*.csv`；
5. `data/parameter_metadata_*.csv` 的状态、来源和不确定度记录。

本表用于集中审阅和参数交接；任何参数替换都应同步修改对应配置/CSV和本文件的来源标记。
