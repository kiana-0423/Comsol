#!/usr/bin/env sh
set -eu

project_home=${PROJECT_HOME:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"}
cd "$project_home"

property() { awk -F= -v key="$2" '$1==key {print substr($0,index($0,"=")+1)}' "$1"; }

validate_triplet() {
  config=$1
  key=$2
  baseline_key=$3
  values=$(property "$config" "$key")
  baseline=$(property "$config" "$baseline_key" | sed 's/\[.*//')
  awk -v file="$config" -v key="$key" -v values="$values" -v baseline="$baseline" '
    BEGIN {
      n=split(values,a,",")
      if (n!=3 || !(a[1]+0<a[2]+0 && a[2]+0<a[3]+0)) {
        print file ": " key " must be a strictly increasing low,baseline,high triplet" > "/dev/stderr"
        exit 1
      }
      if ((a[2]+0)!=(baseline+0)) {
        print file ": middle " key " value does not match " baseline > "/dev/stderr"
        exit 1
      }
    }'
}

for config in config/nfm.properties config/nfmzc.properties; do
  for key in ocv.csv experimental.curve.csv diffusion.charge.csv diffusion.discharge.csv strain.csv parameter.metadata.csv; do
    file=$(property "$config" "$key")
    [ -n "$file" ] && [ -f "$file" ] || { echo "Missing $key referenced by $config: $file" >&2; exit 1; }
  done
  validate_triplet "$config" young.modulus.sensitivity.gpa young.modulus
  validate_triplet "$config" poisson.sensitivity.values poisson.ratio
  validate_triplet "$config" chemical.expansion.beta.sensitivity.values chemical.expansion.beta
  validate_triplet "$config" exchange.current.density.sensitivity.a_m2 exchange.current.density
done

[ "$(property config/full_cell.properties geometry.classification)" = "representative-microstructure" ] || {
  echo 'full_cell geometry.classification must be representative-microstructure' >&2
  exit 1
}
[ -n "$(property config/full_cell.properties negative.chemical.expansion.beta)" ] || {
  echo 'negative.chemical.expansion.beta is missing' >&2
  exit 1
}
validate_triplet config/full_cell.properties \
  negative.exchange.current.density.sensitivity.a_m2 \
  negative.exchange.current.density.baseline.a_m2

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

for file in data/ocv_hard_carbon_template.csv data/k_hard_carbon_template.csv \
            data/ds_hard_carbon_parameters_docx.csv data/electrolyte_conductivity_parameters_docx.csv; do
  awk -F, 'NR==1 {next} NR>2 && ($1+0)<=previous {print FILENAME ": non-increasing x at line " NR; bad=1} {previous=$1+0} END {exit bad}' "$file"
done

echo 'Input audit passed.'
