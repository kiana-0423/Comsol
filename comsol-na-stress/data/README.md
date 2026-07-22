# Interpolation data

The CSV files are editable two-column interpolation tables. Their first column is the independent variable and the second is the value:

- `ds_*`: dimensionless sodium fraction and diffusivity in `m^2/s`; charge and discharge use separate files so measured `D(xNa, direction)` curves can replace the flat GITT-average templates independently. `ds_hard_carbon_parameters_docx.csv` is a provisional digitization of the similar project's `Ds_n(c)` plot, with concentration divided by `csmax_n=14.54 kmol/m³`.
- `strain_*`: sodium fraction and isotropic chemical eigenstrain.
- `ocv_*`: sodium fraction and equilibrium potential in V.
- `experimental_charge_*`: specific capacity in `mAh/g` and measured terminal voltage in V, used for 0.1C calibration checks.
- `k_hard_carbon_*`: sodium fraction and effective Butler–Volmer exchange current density in `A/m²` (the historical filename is retained for compatibility). The active bring-up baseline is 3 A/m² and remains provisional.
- `electrolyte_conductivity_*`: electrolyte concentration in `kmol/m^3` and conductivity in `S/m`. `electrolyte_conductivity_parameters_docx.csv` converts the similar project's plotted mS/cm values to S/m using `1 mS/cm = 0.1 S/m`.
- `parameter_metadata_*`: per-parameter value, unit, status, source and relative uncertainty used for traceability and sensitivity bounds.
- `kang_2026_reference_constraints.csv`: values extracted from Kang et al. (2026) and an explicit decision on whether each value is a shared condition, a validation/sensitivity reference, or non-transferable to NFMZC.

The supplied GITT average diffusivities are encoded in the positive-electrode diffusion tables. The configured hard-carbon and electrolyte curves are the `*_parameters_docx.csv` files; obsolete flat fallback tables have been removed so they cannot be selected accidentally. OCV, strain, hard-carbon and electrolyte tables remain provisional digitization/bring-up data, not measurements. A selected but missing file is a fatal configuration error. Replace these tables with raw experimental data, retain the same two-column schema, update `parameter.status/source/uncertainty`, and preserve the original raw data outside generated output directories.

The Kang paper studies pristine NFM and a **surface-Co-treated NFMC**, not the project's bulk Zn/Co-doped NFMZC. Its pristine-NFM measurements can constrain NFM validation and sensitivity cases. NFMC coating fractions, phase fractions, diffusivity improvements and mechanical response must not be assigned to NFMZC. The reported average GITT value is also not substituted for the supplied charge/discharge-specific GITT values because the averaging definitions and samples differ.

Values explicitly listed in `parameters.docx` and not conflicting with the current project definition are now loaded in the properties files and traced as `parameters.docx legacy COMSOL model` in the metadata tables. They remain `provisional`, because the report does not provide measurement methods or uncertainties. The electrolyte-conductivity and hard-carbon-diffusivity plots have now been digitized as provisional active interpolation tables. The report's 241 mAh/g capacity, shared 46 kmol/m³ positive `csmax`, NFMC diffusivity and ambiguous 1 mol/m³ kinetic reference concentration are deliberately not adopted.

The legacy NFM/HC OCV plots are not activated: they use the old positive initial state `41/46≈0.891`, whereas the current project defines the charged-process start as `x_pos=1`; direct substitution would shift the initialized full-cell voltage substantially. The legacy `k_p/k_n` curves are also not activated because they are reaction-rate constants in `m/s`, while this implementation consumes exchange current density in `A/m²`; conversion requires the exact Butler–Volmer concentration convention and active surface area.
