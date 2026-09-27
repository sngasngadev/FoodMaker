#!/usr/bin/env sh
set -eu
GRADLE_VERSION=9.6.0
CACHE_ROOT="${GRADLE_USER_HOME:-$HOME/.gradle}/foodmaker"
DIST_DIR="$CACHE_ROOT/gradle-$GRADLE_VERSION"
ZIP_PATH="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$CACHE_ROOT"
  if [ ! -f "$ZIP_PATH" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP_PATH"
    else
      wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ZIP_PATH"
    fi
  fi
  unzip -q -o "$ZIP_PATH" -d "$CACHE_ROOT"
fi

exec "$DIST_DIR/bin/gradle" "$@"
