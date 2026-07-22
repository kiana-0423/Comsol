# Interpolation data

The CSV files are editable templates. Their first column is the independent variable and the second is the value:

- `ds_*`: dimensionless sodium fraction and diffusivity in `m^2/s`; charge and discharge use separate files so measured `D(xNa, direction)` curves can replace the flat GITT-average templates independently.
- `strain_*`: sodium fraction and isotropic chemical eigenstrain.
- `ocv_*`: sodium fraction and equilibrium potential in V.
- `experimental_charge_*`: specific capacity in `mAh/g` and measured terminal voltage in V, used for 0.1C calibration checks.
- `k_hard_carbon_*`: sodium fraction and effective Butler–Volmer exchange current density in `A/m²` (the historical filename is retained for compatibility).
- `electrolyte_conductivity_*`: electrolyte concentration in `kmol/m^3` and conductivity in `S/m`.
- `parameter_metadata_*`: per-parameter value, unit, status, source and relative uncertainty used for traceability and sensitivity bounds.

The supplied GITT average diffusivities are encoded in the positive-electrode diffusion templates. OCV, strain, hard-carbon and electrolyte tables remain provisional digitization/bring-up placeholders, not measurements. A selected but missing file is a fatal configuration error. Replace templates with raw experimental data, retain the same two-column schema, update `parameter.status/source/uncertainty`, and preserve the original raw data outside generated output directories.
