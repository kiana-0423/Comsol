package com.nfm.comsol.model;

import com.comsol.model.Model;
import com.comsol.model.util.ModelUtil;
import com.nfm.comsol.config.MaterialConfig;
import com.nfm.comsol.config.SimulationConfig;
import com.nfm.comsol.util.ValidationUtils;

public final class ParticleModelBuilder {
    private final ParameterBuilder parameters = new ParameterBuilder();
    private final GeometryBuilder geometry = new GeometryBuilder();
    private final DefinitionsBuilder definitions = new DefinitionsBuilder();
    private final DiffusionPhysicsBuilder diffusion = new DiffusionPhysicsBuilder();
    private final SolidMechanicsBuilder solid = new SolidMechanicsBuilder();
    private final CouplingBuilder coupling = new CouplingBuilder();
    private final MeshBuilder mesh = new MeshBuilder();
    private final StudyBuilder studies = new StudyBuilder();
    private final ResultBuilder results = new ResultBuilder();

    public BuiltModel build(MaterialConfig material, SimulationConfig simulation,
                            double cRate, String mode, boolean smokeTest) {
        Model model = ModelUtil.create("ParticleModel");
        model.label(material.name() + " axisymmetric Na diffusion-stress model");
        model.comments("Generated exclusively by Java API for COMSOL 6.4. Parameters are provisional.");
        parameters.build(model, material, cRate, mode);
        geometry.build(model);
        definitions.build(model, material);
        diffusion.build(model);
        solid.build(model);
        coupling.build(model);
        mesh.build(model, simulation, smokeTest);
        studies.build(model, simulation, smokeTest);
        results.build(model, simulation);
        ValidationUtils.validateBuiltModel(model);
        return new BuiltModel(model, studies, results);
    }

    public record BuiltModel(Model model, StudyBuilder studies, ResultBuilder results) {}
}
