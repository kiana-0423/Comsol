# COMSOL 6.4 full-cell GUI/API verification

Run this gate on the target Linux or macOS COMSOL 6.4 installation before accepting any full-cell result.

1. Compile with `scripts/compile_unix.sh`, then run `scripts/run_unix.sh --model full-cell --build-only --material NFM --c-rate 1 --mode cycle`.
2. If a Battery Module API identifier fails, create the smallest equivalent node in the 6.4 GUI and use **File → Save As → Model File for Java**. Compare only the failing identifier/property with `ComsolTagUtils` and `FullCellPhysicsBuilder`.
3. Confirm three blocks form one continuous 18 μm representative unit, four hard-carbon particles lie wholly inside the anode without overlap, and the 2.5 μm positive particle lies wholly inside the cathode.
4. Confirm named selections contain: one positive particle domain, four negative particle domains, one separator domain, nonempty internal particle surfaces, and nonempty opposing collectors.
5. Confirm `Lithium-Ion Battery (liion)` contains negative/positive Porous Conductive Binder, Separator, two Internal Electrode Surface nodes, negative 0 V potential and positive total current.
6. Confirm `liion.ies_neg.er1.iloc` and `liion.ies_pos.er1.iloc` exist. Charge must remove Na from the positive particle and insert Na into hard carbon; reverse on discharge.
7. Confirm positive/negative Coefficient Form PDE fields have concentration units and use the local-current/Faraday surface flux, not a second independent imposed flux.
8. Confirm Solid Mechanics is restricted to the positive particle. Chemical strain acts only there, and `RigidMotionSuppression` removes rigid-body modes without pinning expansion. Uniform concentration change in the free particle must not create appreciable deviatoric stress.
9. Confirm current-distribution initialization precedes charge; discharge uses the last charge state. Verify 4.3 V and 2.0 V solver Stop Conditions store the final step. Inspect the time solver and confirm electrochemistry/diffusion precede the displacement field in segregated steps.
10. Run `--smoke-test`; require successful solve/save/export, decreasing positive `xNa` on charge, initial near-zero stress and finite voltage.
11. Run mesh and time convergence. Require average `xNa` <1%, maximum concentration delta <2%, maximum volume-average stress <3%, sampled stress p95 <5%, and mass-balance error <1%. Treat the absolute single-node maximum as diagnostic only.
12. Do not set `parameter.status=measured` or use quantitative stress claims until raw OCV, XRD strain and mechanics data have replaced the templates.
