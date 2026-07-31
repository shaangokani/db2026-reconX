#!/usr/bin/env bash
# =============================================================================
# File: scripts/demo-seed.sh
# Populates a running ReconX stack with realistic data for a live demo.
#
#   bash scripts/demo-seed.sh              # bulk seed, then drip 40 trades live
#   bash scripts/demo-seed.sh --bulk-only  # just fill the tables, no drip
#   bash scripts/demo-seed.sh --live-only  # just drip (dashboard is watching)
#   bash scripts/demo-seed.sh --live 200 --delay 0.5
#
# Why two phases: the Trades / Recon / Audit / DLQ pages read from the database,
# so a bulk seed is enough for them. The Dashboard is SSE-only with no history
# replay — it only shows trades that arrive *while the tab is open* — so the
# drip phase is what makes it visibly tick during a demo.
#
# Safe to re-run: every run uses a fresh random 3-letter trade-ref prefix, so
# it never collides with previous runs (tradeRef is unique).
# =============================================================================
set -uo pipefail

API="${API:-http://localhost:8080/api}"
BULK_TRADES="${BULK_TRADES:-24}"
LIVE_TRADES=100
DELAY=1.2
DO_BULK=1
DO_LIVE=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bulk-only) DO_LIVE=0; shift ;;
    --live-only) DO_BULK=0; shift ;;
    --bulk)      BULK_TRADES="$2"; shift 2 ;;
    --live)      LIVE_TRADES="$2"; shift 2 ;;
    --delay)     DELAY="$2"; shift 2 ;;
    -h|--help)   sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown option: $1 (try --help)"; exit 2 ;;
  esac
done

# EQUITY instruments only (id:symbol:price). The generic Trade row can only be
# converted back to a domain EquityTrade, so these are the ones the recon engine
# can actually process.
INSTRUMENTS=(
  "1:SAP.DE:178.40"  "2:SIE.DE:167.35"  "3:DBKGn.DE:14.85"
  "4:AAPL:227.50"    "5:MSFT:421.30"    "6:GOOGL:172.90"
  "7:HSBA.L:690.10"  "8:VOD.L:71.25"    "9:T.TO:21.60"
)
STATUSES=(MATCHED MATCHED MATCHED UNMATCHED DISPUTED)

# --- unique per-run prefix so re-runs never hit DuplicateTradeRefException ----
LETTERS=({A..Z})
PREFIX="${LETTERS[RANDOM%26]}${LETTERS[RANDOM%26]}${LETTERS[RANDOM%26]}"
TODAY="$(date +%Y%m%d)"
SEQ=0

say()  { printf '%s\n' "$*"; }
step() { printf '\n\033[1m▶ %s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*"; }

json_get() { python -c "import sys,json;d=json.load(sys.stdin);print($1)" 2>/dev/null; }

login() { # $1 email  $2 password
  curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" 2>/dev/null | json_get "d['token']"
}

# -----------------------------------------------------------------------------
step "Checking the stack"
if ! curl -fsS -o /dev/null --max-time 5 "$API/actuator/health/liveness" 2>/dev/null; then
  say "  ✗ backend not reachable at $API"
  say "    start it with:  cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dmaven.test.skip=true"
  exit 1
fi
ok "backend is up at $API"

TRADER=$(login trader@db.com trader123)
ADMIN=$(login admin@db.com admin123)
if [[ -z "${TRADER:-}" || -z "${ADMIN:-}" ]]; then
  say "  ✗ could not log in — are the Day-5 seed users present?"
  exit 1
fi
ok "logged in as trader@db.com and admin@db.com"
ok "trade-ref prefix for this run: ${PREFIX}-${TODAY}-####"

# -----------------------------------------------------------------------------
# NB: callers invoke this via $(...), i.e. in a subshell — anything it assigns is
# discarded when it returns. The sequence number is therefore passed in by the
# caller rather than kept in a global, or every trade would reuse ref #1 and all
# but the first would fail on the unique tradeRef constraint.
create_trade() { # $1 token  $2 seq -> echoes the created id, or nothing on failure
  local token="$1" seq="$2"
  local i=$(( (seq - 1) % ${#INSTRUMENTS[@]} ))
  IFS=':' read -r iid sym base <<< "${INSTRUMENTS[$i]}"
  local ref side qty price cp drift
  ref=$(printf "%s-%s-%04d" "$PREFIX" "$TODAY" "$seq")
  side=$([[ $((seq % 2)) -eq 0 ]] && echo BUY || echo SELL)
  cp=$(( (seq % 10) + 1 ))
  qty=$(( 25 + (seq * 37) % 1200 ))
  # nudge the price +/-2% so the feed doesn't look synthetic
  drift=$(( (seq * 7) % 41 - 20 ))
  price=$(python -c "print(round($base * (1 + $drift/1000.0), 2))")
  curl -fsS -X POST "$API/v1/trades" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"tradeRef\":\"$ref\",\"instrumentId\":$iid,\"counterpartyId\":$cp,
         \"assetClass\":\"EQUITY\",\"side\":\"$side\",\"quantity\":$qty,
         \"price\":$price,\"tradeDate\":\"$(date +%F)\"}" 2>/dev/null \
    | json_get "d['id']"
}

set_status() { # $1 token  $2 id  $3 status
  curl -fsS -o /dev/null -X PATCH "$API/v1/trades/$2/status" \
    -H "Authorization: Bearer $1" -H 'Content-Type: application/json' \
    -d "{\"status\":\"$3\"}" 2>/dev/null
}

# -----------------------------------------------------------------------------
if [[ $DO_BULK -eq 1 ]]; then
  step "Seeding $BULK_TRADES trades (Trades page + audit trail)"
  IDS=()
  failed=0
  for _ in $(seq 1 "$BULK_TRADES"); do
    SEQ=$((SEQ + 1))
    id=$(create_trade "$TRADER" "$SEQ")
    if [[ -n "$id" ]]; then IDS+=("$id"); printf '.'; else failed=$((failed+1)); printf 'x'; fi
  done
  printf '\n'
  ok "created ${#IDS[@]} of $BULK_TRADES trades"
  (( failed )) && warn "$failed failed — check the backend log"

  step "Spreading statuses (Dashboard counters + Trades filter)"
  n=0
  for id in "${IDS[@]}"; do
    # leave roughly a third PENDING so the mix looks real
    if (( n % 3 != 2 )); then
      set_status "$TRADER" "$id" "${STATUSES[$((n % ${#STATUSES[@]}))]}" && printf '.'
    fi
    n=$((n + 1))
  done
  printf '\n'
  ok "statuses applied (MATCHED / UNMATCHED / DISPUTED, rest left PENDING)"

  step "Triggering a reconciliation run"
  if curl -fsS -o /dev/null -X POST "$API/v1/recon/run" \
       -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
       -d "{\"from\":\"2026-01-01\",\"to\":\"$(date +%F)\"}" 2>/dev/null; then
    ok "recon job accepted (drives the ADV084 timer + recon metrics)"
  else
    warn "recon run failed — skipping (not fatal for the demo)"
  fi

  # --- DLQ: needs the Kafka container, so it degrades gracefully -------------
  step "Parking a message on the DLQ (DLQ Admin page)"
  if ! command -v docker >/dev/null 2>&1 || \
     ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^reconx-kafka$'; then
    warn "reconx-kafka container not running — skipping DLQ seed"
    warn "start it with:  docker compose --profile debug up -d zookeeper kafka kafdrop"
  else
    ref=$(printf "%s-%s-%04d" "$PREFIX" "$TODAY" 1)
    eid=$(curl -fsS "$API/v1/audit/trades/$ref/events" \
            -H "Authorization: Bearer $ADMIN" 2>/dev/null | json_get "d[0]['eventId']")
    if [[ -z "${eid:-}" ]]; then
      warn "no audit event found for $ref yet — skipping DLQ seed"
    else
      # Re-publishing a live eventId violates audit_log's unique constraint, so
      # the consumer fails, exhausts its 3 retries and the recoverer parks it.
      docker exec -i reconx-kafka kafka-console-producer \
        --bootstrap-server localhost:9092 --topic trade-events \
        --property parse.key=true --property key.separator='|' >/dev/null 2>&1 <<EOF
$ref|{"eventId":"$eid","tradeRef":"$ref","eventType":"TRADE_CREATED","timestamp":"$(date -u +%FT%TZ)","actor":"demo-seed","before":null,"after":"DUPLICATE"}
EOF
      ok "poison message published — retries run at 1s/2s/4s, then it lands on the DLQ"
      printf '  waiting for the retry cycle'
      for _ in $(seq 1 12); do sleep 1; printf '.'; done; printf '\n'
    fi
  fi
fi

# -----------------------------------------------------------------------------
if [[ $DO_LIVE -eq 1 ]]; then
  step "Live drip: $LIVE_TRADES trades, one every ${DELAY}s"
  say "  Open the Dashboard now — http://localhost:5173/"
  say "  (it is SSE-only with no history replay, so it shows these as they arrive)"
  say "  Ctrl-C to stop early."
  for i in $(seq 1 "$LIVE_TRADES"); do
    SEQ=$((SEQ + 1))
    id=$(create_trade "$TRADER" "$SEQ")
    if [[ -n "$id" ]]; then
      # give ~half of them a terminal status so the counters move too
      (( i % 2 == 0 )) && set_status "$TRADER" "$id" "${STATUSES[$((i % ${#STATUSES[@]}))]}"
      printf '  [%2d/%2d] %s-%s-%04d  id=%s\n' "$i" "$LIVE_TRADES" "$PREFIX" "$TODAY" "$SEQ" "$id"
    else
      warn "trade $i failed"
    fi
    sleep "$DELAY"
  done
  ok "live drip finished"
fi

# -----------------------------------------------------------------------------
step "Where to look"
tot=$(curl -fsS "$API/v1/trades?page=0&size=1" -H "Authorization: Bearer $ADMIN" 2>/dev/null | json_get "d['totalElements']")
brk=$(curl -fsS "$API/v1/recon/jobs/latest/results" -H "Authorization: Bearer $ADMIN" 2>/dev/null | json_get "len(d)")
dlq=$(curl -fsS "$API/v1/admin/dlq" -H "Authorization: Bearer $ADMIN" 2>/dev/null | json_get "len(d)")
printf '  %-34s %s\n' "Trades          http://localhost:5173/trades" "${tot:-?} trades"
printf '  %-34s %s\n' "Recon Breaks    http://localhost:5173/recon"  "${brk:-?} break(s)"
printf '  %-34s %s\n' "DLQ Admin       http://localhost:5173/dlq"    "${dlq:-?} row(s)"
printf '  %-34s %s\n' "Audit Log       http://localhost:5173/audit"  "search: ${PREFIX}-${TODAY}-0001"
printf '  %-34s %s\n' "Dashboard       http://localhost:5173/"       "live feed (SSE)"
echo
