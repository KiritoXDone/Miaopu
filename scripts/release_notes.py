"""Extract one version's release body from the repository changelog."""

import argparse
from pathlib import Path
import re
import sys


def extract_release_notes(changelog: str, version: str) -> str:
    if not re.fullmatch(r"[0-9]+\.[0-9]+(?:\.[0-9]+)?", version):
        raise ValueError("Release version must look like 1.0 or 1.0.1.")

    sections = []
    current = None
    fence = None
    for line in changelog.splitlines():
        if fence is not None:
            closing_fence = rf" {{0,3}}{re.escape(fence[0])}{{{len(fence)},}}[ \t]*"
            if re.fullmatch(closing_fence, line):
                fence = None
        else:
            opening = re.match(r" {0,3}(`{3,}|~{3,})", line)
            heading = re.fullmatch(r"##[ \t]+(.+?)[ \t]*", line)
            if opening:
                fence = opening.group(1)
            elif heading:
                current = [] if heading.group(1) == version else None
                if current is not None:
                    sections.append(current)
                continue
        if current is not None:
            current.append(line)

    if len(sections) != 1:
        raise ValueError(f"Expected exactly one '## {version}' section; found {len(sections)}.")
    notes = "\n".join(sections[0]).strip()
    if not notes:
        raise ValueError(f"The '## {version}' section is empty.")
    return notes + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("--changelog", type=Path, default=Path("changelog.md"))
    args = parser.parse_args()
    try:
        notes = extract_release_notes(args.changelog.read_text(encoding="utf-8"), args.version)
    except (OSError, UnicodeError, ValueError) as error:
        print(f"Release notes error: {error}", file=sys.stderr)
        return 1
    sys.stdout.write(notes)
    return 0


if __name__ == "__main__":
    sys.exit(main())
