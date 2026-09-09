#!/bin/sh
# ExperientialLabs gateway health check — bypasses opencode entirely.
# Usage: scripts/explabs-check.sh
# Needs EXPLABS_API_KEY in .env (project root) or exported in the shell.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ -z "${EXPLABS_API_KEY:-}" ] && [ -f "$ROOT/.env" ]; then
  set -a; . "$ROOT/.env"; set +a
fi
if [ -z "${EXPLABS_API_KEY:-}" ]; then
  echo "EXPLABS_API_KEY is not set (and $ROOT/.env missing it). Aborting."
  exit 1
fi
BASE="https://api.experientiallabs.ai"

echo "--- whoami ---"
curl -s -o /tmp/explabs-whoami.json -w "http:%{http_code} time:%{time_total}s\n" \
  "$BASE/api/whoami" -H "Authorization: Bearer $EXPLABS_API_KEY"
head -c 200 /tmp/explabs-whoami.json; echo; echo

for MODEL in gpt-6-astra gpt-5.6-luna; do
  echo "--- chat $MODEL ---"
  curl -s -o /tmp/explabs-chat.json -w "http:%{http_code} time:%{time_total}s\n" \
    --max-time 60 "$BASE/v1/chat/completions" \
    -H "Authorization: Bearer $EXPLABS_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"model\": \"$MODEL\", \"messages\": [{\"role\": \"user\", \"content\": \"Reply with exactly: ok\"}]}"
  python3 -c "
import json
d = json.load(open('/tmp/explabs-chat.json'))
if 'error' in d:
    e = d['error']; print('ERROR', e.get('code'), '|', (e.get('message') or '')[:160])
else:
    print('REPLY:', d['choices'][0]['message']['content'][:200])
"
  echo
done
