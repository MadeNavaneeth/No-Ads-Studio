#!/usr/bin/env zsh
# Run a single C, C++ or Python source file — build it, run it, show the output.
# Compilers already on macOS: Apple clang (gcc/g++ aliases) and python3.
#
# Usage: scripts/run-file.sh <file> [args passed to the program...]
#   scripts/run-file.sh scratch/hello.py
#   scripts/run-file.sh scratch/hello.c
#   scripts/run-file.sh scratch/hello.cpp --flag value
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <file.(c|cpp|cc|cxx|py)> [args...]" >&2
  exit 2
fi

SRC="$1"; shift
if [[ ! -f "$SRC" ]]; then
  echo "no such file: $SRC" >&2
  exit 2
fi

EXT="${SRC##*.}"
BIN_DIR=".run"
mkdir -p "$BIN_DIR"

case "$EXT" in
  py)
    exec python3 "$SRC" "$@"
    ;;
  c)
    OUT="$BIN_DIR/${SRC:t:r}"           # basename without extension
    cc -O2 -std=c17 -Wall -Wextra -o "$OUT" "$SRC"
    ;;
  cpp|cc|cxx)
    OUT="$BIN_DIR/${SRC:t:r}"
    c++ -O2 -std=c++17 -Wall -Wextra -o "$OUT" "$SRC"
    ;;
  *)
    echo "unsupported extension: .$EXT (want .py, .c, .cpp, .cc or .cxx)" >&2
    exit 2
    ;;
esac

exec "$OUT" "$@"
