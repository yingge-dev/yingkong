#!/system/bin/sh
DATA=/data/adb/yingkong
echo $$ > "$DATA/pid"
while true; do
  if [ -f "$DATA/disabled" ]; then
    exit 0
  fi
  {
    echo "ts=$(date +%s)"
    echo "uptime=$(cut -d' ' -f1 /proc/uptime 2>/dev/null)"
    echo "battery=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null)"
    echo "temp=$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null)"
    echo "load=$(cat /proc/loadavg 2>/dev/null)"
  } > "$DATA/status.tmp"
  mv "$DATA/status.tmp" "$DATA/status"
  sleep 5
done
