#!/bin/sh
# Minden változáskor újrafordítja a Java forrásokat, így a Spring DevTools (ami csak
# a target/classes-t figyeli, a .java fájlokat nem) észleli a változást és újraindul.
MARKER=/tmp/.last-compile
touch "$MARKER"
while true; do
  if [ -n "$(find src -name '*.java' -newer "$MARKER")" ]; then
    touch "$MARKER"
    mvn -o -q compile
  fi
  sleep 1
done
