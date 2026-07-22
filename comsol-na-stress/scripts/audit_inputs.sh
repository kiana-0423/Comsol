#!/usr/bin/env sh
set -eu

project_home=${PROJECT_HOME:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"}
cd "$project_home"

property() { awk -F= -v key="$2" '$1==key {print substr($0,index($0,"=")+1)}' "$1"; }

for config in config/nfm.properties config/nfmzc.properties; do
  for key in ocv.csv experimental.curve.csv diffusion.charge.csv diffusion.discharge.csv strain.csv parameter.metadata.csv; do
    file=$(property "$config" "$key")
    [ -n "$file" ] && [ -f "$file" ] || { echo "Missing $key referenced by $config: $file" >&2; exit 1; }
  done
done

for key in negative.ocv.csv negative.kinetics.csv negative.diffusivity.csv electrolyte.conductivity.csv parameter.metadata.csv; do
  file=$(property config/full_cell.properties "$key")
  [ -n "$file" ] && [ -f "$file" ] || { echo "Missing $key referenced by full_cell.properties: $file" >&2; exit 1; }
done

for file in data/*.csv; do
  awk -F, 'NR==1 {columns=NF; next} NF!=columns {print FILENAME ": inconsistent columns at line " NR; bad=1} END {if (NR<3) bad=1; exit bad}' "$file"
done

for file in data/parameter_metadata_*.csv; do
  awk -F, 'NR==1 {if ($0!="parameter,value,unit,status,source,relative_uncertainty") bad=1; next} NF!=6 {bad=1} $4!="provisional" && $4!="literature" && $4!="measured" {bad=1} ($6+0)<0 || ($6+0)>1 {bad=1} END {exit bad}' "$file"
done

echo 'Input audit passed.'
