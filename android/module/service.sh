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
chmod 755 "$MODDIR/bin/"* 2>/dev/null
case "$(uname -m)" in
  aarch64) BIN="$MODDIR/bin/yingkong-daemon-arm64" ;;
  armv7l|armv8l) BIN="$MODDIR/bin/yingkong-daemon-armv7" ;;
  x86_64) BIN="$MODDIR/bin/yingkong-daemon-x86_64" ;;
  *) BIN="" ;;
esac
if [ -n "$BIN" ] && [ -x "$BIN" ]; then
  "$BIN" >"$DATA/daemon.log" 2>&1 &
  sleep 1
  if [ ! -f "$DATA/pid" ] || ! kill -0 $(cat "$DATA/pid") 2>/dev/null; then
    sh "$MODDIR/bin/yingkong-daemon.sh" >"$DATA/daemon.log" 2>&1 &
  fi
else
  sh "$MODDIR/bin/yingkong-daemon.sh" >"$DATA/daemon.log" 2>&1 &
fi
