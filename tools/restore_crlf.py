#!/usr/bin/env python3
from __future__ import annotations

import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
paths = list(root.glob('*.yml')) + list((root / 'scripts').rglob('*.js'))
changed = 0
for path in paths:
    data = path.read_bytes()
    # The upstream Magic RSC data/script files are CRLF. The translation pass
    # changed text but should not create a repository-wide EOL-only diff.
    normalized = data.replace(b'\r\n', b'\n').replace(b'\n', b'\r\n')
    if normalized != data:
        path.write_bytes(normalized)
        changed += 1
        print(f'restored CRLF: {path.relative_to(root)}')
print(f'CRLF restored in {changed} files')
