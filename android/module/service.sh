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
sh "$MODDIR/bin/yingkong-daemon" >"$DATA/daemon.log" 2>&1 &
