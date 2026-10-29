#!/usr/bin/env python3

from __future__ import annotations

import argparse
from pathlib import Path

from summary_extract import extract_dude_summary
from summary_render import render_summary


def build_missing_payload() -> dict[str, object]:
    return {
        'tool': 'dude',
        'status': 'missing',
        'metadata': {},
        'markdown': '\n'.join([
            '## DuDe',
            '',
            '- Summary input is missing',
        ]),
        'templateModel': {
            'isMissing': True,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        prog='dude-summary.py',
        description='Generates DuDe summary artifacts for Voyager',
    )
    parser.add_argument('results_directory', nargs='?', default='results')
    args = parser.parse_args()

    target_directory = Path(args.results_directory).resolve()

    try:
        internal_files = list(target_directory.glob('*-internal_duplication.json'))
        external_files = list(target_directory.glob('*-external_duplication.csv'))

        if len(internal_files) == 0 and len(external_files) == 0:
            print(
                "summary input missing for dude: expected '*-internal_duplication.json' or "
                f"'*-external_duplication.csv' in '{target_directory}'; generating missing summary artifacts"
            )
            payload = build_missing_payload()
        else:
            payload = extract_dude_summary(target_directory)

        rendered = render_summary(target_directory, payload)

        print(f"Generated summary markdown at {rendered['summaryMdPath']}")
        print(f"Generated summary html at {rendered['summaryHtmlPath']}")
        return 0
    except Exception as error:
        print(f"summary generation failed for '{target_directory}': {error}")
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
