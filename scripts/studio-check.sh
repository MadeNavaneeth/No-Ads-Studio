#!/usr/bin/env zsh
# studio-check.sh — local CI for the whole studio: the conformance gate plus
# every project's `check`, in one command. No cloud, no rate limits — this
# machine is the runner. The pre-push hook (.githooks/) invokes it, so nothing
# reaches GitHub unverified.
#
# Usage:
#   scripts/studio-check.sh                 full run: gate + every project's check
#   scripts/studio-check.sh --quick         gate only (seconds)
#   scripts/studio-check.sh --fail-fast     stop at the first red project
#   scripts/studio-check.sh --only=a,b      check only these projects (gate always
#                                           runs; names as under projects/)
#   STUDIO_CHECK_SKIP=kakuro-app scripts/studio-check.sh      skip in-flight projects
#
# Per-project Gradle output lands in .studio-check-logs/ (gitignored); on a red
# project its tail is printed. Exit code 0 only if everything ran green.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

quick=0
fail_fast=0
only=""
for arg in "$@"; do
  case "$arg" in
    --quick) quick=1 ;;
    --fail-fast) fail_fast=1 ;;
    --only=*) only="${arg#--only=}" ;;
    *) echo "unknown flag: $arg" >&2; exit 2 ;;
  esac
done

# ── palette ───────────────────────────────────────────────────────────────────
if [[ -t 1 ]]; then
  G=$'\e[32m'; R=$'\e[31m'; B=$'\e[1m'; D=$'\e[2m'; X=$'\e[0m'
else
  G=""; R=""; B=""; D=""; X=""
fi

# ── discover projects: any directory under projects/ with its own wrapper ────
typeset -a projects
for d in projects/*/; do
  [[ -x "$d/gradlew" ]] || continue
  p="${d#projects/}"; p="${p%/}"
  projects+=("$p")
done
(( ${#projects} > 0 )) || { echo "no gradle projects found under projects/" >&2; exit 1; }

# --only filters; STUDIO_CHECK_SKIP removes (comma lists)
if [[ -n "$only" ]]; then
  typeset -a wanted
  wanted=("${(@s/,/)only}")
  typeset -a filtered
  for p in $projects; do
    for w in $wanted; do [[ "$p" == "$w" ]] && filtered+=("$p")
    done
  done
  projects=("$filtered[@]")
  (( ${#projects} > 0 )) || { echo "--only matched no projects: $only" >&2; exit 2; }
fi
if [[ -n "${STUDIO_CHECK_SKIP:-}" ]]; then
  for s in "${(@s/,/)STUDIO_CHECK_SKIP}"; do
    typeset -a kept
    for p in $projects; do [[ "$p" != "$s" ]] && kept+=("$p")
    done
    projects=("$kept[@]")
  done
fi

LOGDIR=".studio-check-logs"
mkdir -p "$LOGDIR"

# ── the gate: run through the first project wired to ../../conformance ───────
gate_project=""
for p in $projects; do
  if grep -q "includeBuild('../../conformance')" "projects/$p/settings.gradle" 2>/dev/null; then
    gate_project="$p"; break
  fi
done

run_gate() {
  if [[ -z "$gate_project" ]]; then
    echo "${D}gate: no project wires ../../conformance — skipped$X"
    return 0
  fi
  ( cd "projects/$gate_project" && ./gradlew :conformance:conformanceCheck --console=plain ) \
    > "$LOGDIR/gate.log" 2>&1
}

# ── run ───────────────────────────────────────────────────────────────────────
typeset -a order green red
seconds_start=$SECONDS
gate_t0=$SECONDS
printf '%s▸ gate%s (conformanceCheck via %s)\n' "$B" "$X" "${gate_project:-—}"
if run_gate; then
  green+=("gate")
  printf '  %s✓ gate%s %s%ss%s\n' "$G" "$X" "$D" "$(( SECONDS - gate_t0 ))" "$X"
else
  red+=("gate")
  echo "  ${R}✗ gate${X}"
  tail -25 "$LOGDIR/gate.log"
  (( fail_fast )) && { echo "${R}STUDIO CHECK FAILED (fail-fast at gate)${X}"; exit 1; }
fi

if (( ! quick )); then
  for p in $projects; do
    t0=$SECONDS
    printf '%s▸ %s%s\n' "$B" "$p" "$X"
    if ( cd "projects/$p" && ./gradlew check --console=plain ) > "$LOGDIR/$p.log" 2>&1; then
      green+=("$p")
      printf '  %s✓ %s%s %s%ss%s\n' "$G" "$p" "$X" "$D" "$(( SECONDS - t0 ))" "$X"
    else
      red+=("$p")
      printf '  %s✗ %s%s\n' "$R" "$p" "$X"
      tail -25 "$LOGDIR/$p.log"
      (( fail_fast )) && break
    fi
  done
fi

# ── verdict ───────────────────────────────────────────────────────────────────
elapsed=$(( SECONDS - seconds_start ))
printf '\n%s── studio check ─ %ss ──────────────────────────%s\n' "$D" "$elapsed" "$X"
for g in $green; do printf '  %s✓ %s%s\n' "$G" "$g" "$X"; done
for r in $red;   do printf '  %s✗ %s%s\n' "$R" "$r" "$X"; done
if (( ${#red} == 0 )); then
  printf '%sSTUDIO CHECK PASSED%s\n' "$G" "$X"
  exit 0
else
  printf '%sSTUDIO CHECK FAILED — %d red%s (logs: %s/)\n' "$R" "${#red}" "$X" "$LOGDIR"
  exit 1
fi
