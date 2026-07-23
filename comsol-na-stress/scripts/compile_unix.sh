#!/usr/bin/env sh
set -eu

project_home=${PROJECT_HOME:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"}

find_comsol() {
  if [ -n "${COMSOL_HOME:-}" ] && [ -x "$COMSOL_HOME/bin/comsol" ]; then
    printf '%s\n' "$COMSOL_HOME/bin/comsol"
    return
  fi
  if command -v comsol >/dev/null 2>&1; then
    command -v comsol
    return
  fi
  for candidate in \
    /Applications/COMSOL64/Multiphysics/bin/comsol \
    "/Applications/COMSOL Multiphysics 6.4.app/Contents/Resources/bin/comsol" \
    /usr/local/comsol64/multiphysics/bin/comsol \
    /opt/comsol64/multiphysics/bin/comsol; do
    if [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return; fi
  done
  return 1
}

comsol_cmd=$(find_comsol) || {
  echo 'ERROR: COMSOL 6.4 command not found. Set COMSOL_HOME to the Multiphysics installation directory.' >&2
  exit 2
}

classes="$project_home/target/classes"
source_root="$project_home/src/main/java"
main_source="com/nfm/comsol/Main.java"
main_class="$classes/com/nfm/comsol/Main.class"
mkdir -p "$classes"
[ -n "$(find "$source_root" -type f -name '*.java' -print -quit)" ] \
  || { echo 'ERROR: no Java sources found.' >&2; exit 3; }
[ -f "$source_root/$main_source" ] \
  || { echo "ERROR: main source not found: $source_root/$main_source" >&2; exit 4; }

# COMSOL compile accepts one Java target and does not recursively compile this
# multi-file project's source dependencies. Compile in dependency order, adding
# the completed output tree to the next invocation's classpath.
compile_one() {
  source=$1
  class_file="$classes/${source%.java}.class"
  rm -f "$class_file"
  (
    cd "$source_root"
    "$comsol_cmd" compile -classpathadd "$classes" -d "$classes" "$source"
  )
  [ -f "$class_file" ] || {
    echo "ERROR: class was not generated for $source" >&2
    echo 'Inspect the latest COMSOL compile log under ~/Library/Preferences/COMSOL/v64/logs on macOS.' >&2
    exit 5
  }
}

# Remove stale bytecode first because some COMSOL launchers return success even
# after printing a Java compilation error.
find "$classes" -type f -name '*.class' -delete

# Layer 1: dependency-free project types.
for source in \
  com/nfm/comsol/config/MaterialConfig.java \
  com/nfm/comsol/config/SimulationConfig.java \
  com/nfm/comsol/config/FullCellConfig.java \
  com/nfm/comsol/util/ComsolTagUtils.java \
  com/nfm/comsol/util/PathUtils.java \
  com/nfm/comsol/fullcell/SensitivityCase.java
do
  compile_one "$source"
done

# Layer 2: validation and configuration loading.
compile_one com/nfm/comsol/util/ValidationUtils.java
compile_one com/nfm/comsol/config/ConfigLoader.java

# Layer 3: model nodes and exporters that depend only on layers 1-2.
for source in \
  com/nfm/comsol/model/CouplingBuilder.java \
  com/nfm/comsol/model/DefinitionsBuilder.java \
  com/nfm/comsol/model/DiffusionPhysicsBuilder.java \
  com/nfm/comsol/model/GeometryBuilder.java \
  com/nfm/comsol/model/MeshBuilder.java \
  com/nfm/comsol/model/ParameterBuilder.java \
  com/nfm/comsol/model/ResultBuilder.java \
  com/nfm/comsol/model/SolidMechanicsBuilder.java \
  com/nfm/comsol/model/StudyBuilder.java \
  com/nfm/comsol/export/CsvExporter.java \
  com/nfm/comsol/export/FigureExporter.java \
  com/nfm/comsol/export/MetricsExporter.java \
  com/nfm/comsol/fullcell/ComparisonFigureComposer.java \
  com/nfm/comsol/fullcell/FullCellDefinitionsBuilder.java \
  com/nfm/comsol/fullcell/FullCellGeometryBuilder.java \
  com/nfm/comsol/fullcell/FullCellMeshBuilder.java \
  com/nfm/comsol/fullcell/FullCellParameterBuilder.java \
  com/nfm/comsol/fullcell/FullCellPhysicsBuilder.java \
  com/nfm/comsol/fullcell/FullCellResultBuilder.java \
  com/nfm/comsol/fullcell/FullCellStudyBuilder.java
do
  compile_one "$source"
done
compile_one com/nfm/comsol/fullcell/FullCellExporter.java

# Layer 4: composed model builders.
compile_one com/nfm/comsol/model/ParticleModelBuilder.java
compile_one com/nfm/comsol/fullcell/FullCellModelBuilder.java

# Layer 5: simulation runners and batch orchestration.
compile_one com/nfm/comsol/runner/SimulationRunner.java
compile_one com/nfm/comsol/fullcell/FullCellSimulationRunner.java
compile_one com/nfm/comsol/runner/BatchRunner.java
compile_one com/nfm/comsol/fullcell/FullCellBatchRunner.java

# Layer 6: command-line entry point.
compile_one "$main_source"
[ -f "$main_class" ] || { echo 'ERROR: Main.class was not generated.' >&2; exit 6; }
echo "Compiled classes: $classes"
