#!/system/bin/sh
if [ -f /data/adb/yingkong/pid ]; then
  kill -9 $(cat /data/adb/yingkong/pid) 2>/dev/null
fi
rm -rf /data/adb/yingkong
