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

# COMSOL compile accepts one Java target. Run it from the package source root so
# javac can discover all com.nfm.* source dependencies through their package paths.
# Remove stale bytecode first because some COMSOL launchers can return success even
# after printing a Java compilation error.
find "$classes" -type f -name '*.class' -delete
(
  cd "$source_root"
  "$comsol_cmd" compile -classpathadd "$classes" -d "$classes" "$main_source"
)
[ -f "$main_class" ] || {
  echo 'ERROR: COMSOL reported completion but Main.class was not generated.' >&2
  echo 'Inspect the COMSOL compile log under ~/Library/Preferences/COMSOL/v64/logs on macOS.' >&2
  exit 5
}
echo "Compiled classes: $classes"
