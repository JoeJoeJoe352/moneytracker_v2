#!/bin/sh
# Recompiles Java sources on every change so Spring DevTools (which only
# watches target/classes, not .java files) picks up the change and restarts.
#
# Uses polling instead of inotify: the source directory is bind-mounted from
# the Windows host, and Docker Desktop does not propagate real inotify events
# across that mount, so an event-based watcher (e.g. entr) never fires.
MARKER=/tmp/.last-compile
touch "$MARKER"
while true; do
  if [ -n "$(find src -name '*.java' -newer "$MARKER")" ]; then
    touch "$MARKER"
    mvn -o -q compile
  fi
  sleep 1
done
