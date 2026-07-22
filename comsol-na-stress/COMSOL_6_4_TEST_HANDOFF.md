# COMSOL 6.4 Linux / macOS 测试交接

## 适用范围

本工程只在 Linux 或 macOS 的 COMSOL Multiphysics 6.4 上编译和运行。目标机器需要可用的 Battery Module 与 Structural Mechanics Module 许可证。所有命令均在工程根目录执行。

如 COMSOL 不在默认路径，先设置安装目录，例如：

```sh
export COMSOL_HOME=/opt/comsol64/multiphysics
```

macOS 可改为实际的应用安装目录。脚本也会自动检查常见的 Linux/macOS 安装位置。

## 固定测试顺序

### 1. 输入审计和编译

```sh
scripts/audit_inputs.sh
scripts/compile_unix.sh
```

预期：输入审计通过；Java API 源码由 COMSOL 6.4 自带的编译入口编译到 `target/classes`。

### 2. 二维颗粒基准

```sh
scripts/run_unix.sh --model particle --material NFM --c-rate 1 --mode charge --build-only
scripts/run_unix.sh --model particle --smoke-test
scripts/run_unix.sh --model particle --material NFM --c-rate 1 --mode charge --solve
scripts/run_unix.sh --model particle --material NFM --c-rate 1 --mode discharge --solve
```

先确认球形扩散方向、库存守恒、初始零应力和自由颗粒约束。此门未通过时不要运行三维模型。

### 3. 三维 API 构建门

```sh
scripts/run_unix.sh --model full-cell --material NFM --c-rate 1 --mode cycle --build-only
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode cycle --build-only
scripts/run_unix.sh --model full-cell --smoke-test
```

用 GUI 打开生成的 `.mph`，逐项执行 [`FULL_CELL_GUI_CHECKLIST.md`](FULL_CELL_GUI_CHECKLIST.md)。若 Battery Module 节点或属性名在本机 6.4 安装中报错，用 GUI 建立最小等价节点并“另存为 Java 模型”，只校正报错的 API 标识。

### 4. 0.1C 校准工况

```sh
scripts/run_unix.sh --model full-cell --material NFM --c-rate 0.1 --mode cycle --solve
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 0.1 --mode cycle --solve
```

检查电压—容量曲线、4.3/2.0 V 截止、充放电连续初值、容量误差和 Na 物料平衡。当前 OCV 为图片数字化 provisional 数据，因此 `100 mV` 是 bring-up 警戒线，不是论文验收标准。

### 5. 1C 独立验证

```sh
scripts/run_unix.sh --model full-cell --material NFM --c-rate 1 --mode cycle --solve
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode cycle --solve
```

核对充电平均浓度单调下降、放电单调上升，快照电压与实际解层索引合理，并检查 NFM/NFMZC 使用相同色标。

### 6. 收敛性

```sh
scripts/run_unix.sh --model full-cell --material NFM --c-rate 1 --mode charge --mesh-convergence
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --mesh-convergence
scripts/run_unix.sh --model full-cell --material NFM --c-rate 1 --mode charge --time-convergence
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --time-convergence
```

必须满足项目配置中的浓度、应力、时间步和物料平衡阈值。若失败，先修正网格/时间步，再重复第 4、5 步。

### 7. 敏感性和材料差异归因

```sh
scripts/run_unix.sh --model full-cell --material NFM --c-rate 1 --mode charge --parameter-sensitivity
scripts/run_unix.sh --model full-cell --material NFMZC --c-rate 1 --mode charge --parameter-sensitivity
scripts/run_unix.sh --model full-cell --c-rate 1 --mode cycle --parameter-attribution
```

敏感性采用 `config/nfm.properties`、`config/nfmzc.properties` 和 `config/full_cell.properties` 中的三点范围，正极与硬碳界面动力学独立缩放。归因计算必须把扩散率、化学膨胀系数、杨氏模量和正极动力学的影响分别输出，避免把 NFMZC 的全部变化归因于单一参数。

## 交付物检查

每个正式工况至少应有：

- `output/mph/` 中的模型；
- `output/csv/` 中的电压/容量、浓度、应力、快照索引和验收表；
- `output/figures/` 中统一色标的浓度/应力图；
- `output/logs/` 中完整运行日志和参数快照。

只有在以下条件同时成立后才进入论文定量汇总：API/GUI 节点核验通过、0.1C 校准与 1C 验证完成、网格和时间步收敛、物料平衡通过，并且 OCV、XRD 化学应变及力学参数已由可追溯原始数据替换。此前所有峰值应力都必须标为 `provisional`。
