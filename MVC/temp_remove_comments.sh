#!/bin/bash

# Path to the JavaScript file
JS_FILE="src/main/resources/static/js/admin/completed_services.js"

# Create a temporary file
TMP_FILE=$(mktemp)

# Remove multi-line comments (/**...*/)
sed -E ':a;N;$!ba;s|/\*\*[\s\S]*?\*/||g' "$JS_FILE" > "$TMP_FILE"

# Remove single-line comments (//)
sed -E 's|//.*$||g' "$TMP_FILE" > "$JS_FILE"

# Remove empty lines
sed -i '/^[[:space:]]*$/d' "$JS_FILE"

echo "All comments have been removed from $JS_FILE"