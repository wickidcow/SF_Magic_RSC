#!/usr/bin/env python3
from __future__ import annotations

import pathlib
import subprocess

root = pathlib.Path(__file__).resolve().parents[1]
paths = list(root.glob('*.yml')) + list((root / 'scripts').rglob('*.js'))
changed = 0
for path in paths:
    rel = path.relative_to(root).as_posix()
    try:
        upstream = subprocess.check_output(['git', 'show', f'origin/main:{rel}'])
    except subprocess.CalledProcessError:
        continue

    data = path.read_bytes()
    # Match the exact newline convention used by this file on main. Magic's
    # repository mixes CRLF and LF, so a global conversion creates noisy diffs.
    upstream_without_crlf = upstream.replace(b'\r\n', b'')
    uses_crlf = b'\r\n' in upstream and b'\n' not in upstream_without_crlf

    canonical = data.replace(b'\r\n', b'\n')
    normalized = canonical.replace(b'\n', b'\r\n') if uses_crlf else canonical
    if normalized != data:
        path.write_bytes(normalized)
        changed += 1
        print(f"matched {'CRLF' if uses_crlf else 'LF'}: {rel}")

print(f'line endings matched main in {changed} files')
