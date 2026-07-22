#!/usr/bin/env sh
set -eu

project_home=${PROJECT_HOME:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"}

find_comsol() {
  if [ -n "${COMSOL_HOME:-}" ] && [ -x "$COMSOL_HOME/bin/comsol" ]; then
    printf '%s\n' "$COMSOL_HOME/bin/comsol"
    return
  fi
  if command -v comsol >/dev/null 2>&1; then command -v comsol; return; fi
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
main_class="$classes/com/nfm/comsol/Main.class"
[ -f "$main_class" ] || {
  echo 'ERROR: compiled classes not found. Run scripts/compile_unix.sh first.' >&2
  exit 3
}

encoded_args=
for arg in "$@"; do
  case "$arg" in *\"*) echo 'ERROR: arguments containing a double quote are unsupported.' >&2; exit 4;; esac
  encoded_args="$encoded_args \"$arg\""
done

cd "$project_home"
NFM_COMSOL_ARGS=${encoded_args# } "$comsol_cmd" batch \
  -classpathadd "$classes" \
  -inputfile "$main_class"
