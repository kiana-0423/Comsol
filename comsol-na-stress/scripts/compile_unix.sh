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
application_class="$classes/com/nfm/comsol/Main.class"
batch_entry_source="NfmComsolEntry.java"
batch_entry_class="$classes/NfmComsolEntry.class"
mkdir -p "$classes"
[ -n "$(find "$source_root" -type f -name '*.java' -print -quit)" ] \
  || { echo 'ERROR: no Java sources found.' >&2; exit 3; }
[ -f "$source_root/$main_source" ] \
  || { echo "ERROR: main source not found: $source_root/$main_source" >&2; exit 4; }
[ -f "$source_root/$batch_entry_source" ] \
  || { echo "ERROR: COMSOL batch entry source not found: $source_root/$batch_entry_source" >&2; exit 4; }

# Resolve the installation root from the COMSOL launcher. COMSOL's `compile`
# command is designed for a single exported Model Java file and can fail with
# "Unable to save compilation result" for a packaged, multi-file application.
# COMSOL 6.4 already ships the Eclipse batch compiler used by that command, so
# invoke it directly and let it compile the complete source set in one pass.
if [ -n "${COMSOL_HOME:-}" ]; then
  comsol_root=$COMSOL_HOME
else
  comsol_root=$(CDPATH= cd -- "$(dirname -- "$comsol_cmd")/.." && pwd)
fi

java_cmd=$(find "$comsol_root/java" -type f -name java -perm -111 -print -quit 2>/dev/null || true)
ecj_jar=$(find "$comsol_root/plugins" -maxdepth 1 -type f \
  -name 'org.eclipse.jdt.core.compiler.batch_*.jar' -print -quit 2>/dev/null || true)

[ -n "$java_cmd" ] && [ -x "$java_cmd" ] || {
  echo "ERROR: COMSOL bundled Java runtime not found below: $comsol_root/java" >&2
  exit 5
}
[ -n "$ecj_jar" ] && [ -f "$ecj_jar" ] || {
  echo "ERROR: COMSOL Eclipse compiler bundle not found below: $comsol_root/plugins" >&2
  exit 6
}

plugin_classpath=$(find "$comsol_root/plugins" -maxdepth 1 -type f -name '*.jar' -print \
  | LC_ALL=C sort | paste -sd ':' -)
[ -n "$plugin_classpath" ] || {
  echo "ERROR: no COMSOL API jars found below: $comsol_root/plugins" >&2
  exit 7
}
compile_classpath="$classes:$plugin_classpath"

# Remove stale bytecode because it could otherwise mask a failed compilation.
find "$classes" -type f -name '*.class' -delete

find "$source_root" -type f -name '*.java' -exec \
  "$java_cmd" -cp "$ecj_jar" org.eclipse.jdt.internal.compiler.batch.Main \
  -source 8 -target 8 -encoding UTF-8 \
  -classpath "$compile_classpath" -d "$classes" {} +

[ -f "$application_class" ] || { echo 'ERROR: packaged application class was not generated.' >&2; exit 8; }
[ -f "$batch_entry_class" ] || { echo 'ERROR: COMSOL batch entry class was not generated.' >&2; exit 8; }
echo "Compiler: $ecj_jar"
echo "Compiled classes: $classes"
