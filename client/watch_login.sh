#!/bin/bash
OLD=106.51.65.110
echo "watching for IP change from $OLD (enable USB tethering now)..."
for i in $(seq 1 60); do
  IP=$(curl -s --max-time 8 https://api.ipify.org 2>/dev/null)
  if [ -n "$IP" ] && [ "$IP" != "$OLD" ]; then
    echo "IP changed to $IP — running login"
    node dologin.js
    exit 0
  fi
  sleep 10
done
echo "timed out after 10 min — tethering IP never changed (still $OLD)"
