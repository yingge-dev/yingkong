#!/system/bin/sh
MODDIR=${0%/*}
DATA=/data/adb/yingkong
mkdir -p "$DATA"
if [ -f "$DATA/disabled" ]; then
  exit 0
fi
if [ -f "$DATA/pid" ]; then
  kill -9 $(cat "$DATA/pid") 2>/dev/null
fi
rm -f "$DATA/pid"
BIN="$MODDIR/bin/yingkong-daemon"
chmod 755 "$BIN" "$MODDIR/bin/yingkong-daemon.sh" 2>/dev/null
if [ "$(uname -m)" = "aarch64" ] && [ -x "$BIN" ]; then
  "$BIN" >"$DATA/daemon.log" 2>&1 &
  sleep 1
  if [ ! -f "$DATA/pid" ] || ! kill -0 $(cat "$DATA/pid") 2>/dev/null; then
    sh "$MODDIR/bin/yingkong-daemon.sh" >"$DATA/daemon.log" 2>&1 &
  fi
else
  sh "$MODDIR/bin/yingkong-daemon.sh" >"$DATA/daemon.log" 2>&1 &
fi

