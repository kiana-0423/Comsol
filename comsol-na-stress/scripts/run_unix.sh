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
main_class="$classes/NfmComsolEntry.class"
[ -f "$main_class" ] || {
  echo 'ERROR: compiled classes not found. Run scripts/compile_unix.sh first.' >&2
  exit 3
}

args_file="$project_home/target/nfm_comsol_args.txt"
comsol_tmp="${COMSOL_TMPDIR:-$project_home/target/comsol-tmp}"
mkdir -p "$project_home/target"
mkdir -p "$comsol_tmp"
: > "$args_file"
for arg in "$@"; do
  case "$arg" in
    *'
'*) echo 'ERROR: arguments containing a newline are unsupported.' >&2; exit 4;;
  esac
  printf '%s\n' "$arg" >> "$args_file"
done

cd "$project_home"
echo "COMSOL batch entry: $main_class"
# This reviewed local application reads project properties/CSV files and writes
# MPH/CSV/PNG/log outputs. COMSOL otherwise applies its application-method
# sandbox to class-file batch entries and denies those required file accesses.
"$comsol_cmd" -Dcs.enablesecurity=off -3drend "${COMSOL_3D_RENDERER:-sw}" \
  -tmpdir "$comsol_tmp" batch \
  -classpathadd "$classes" \
  -inputfile "$main_class"
