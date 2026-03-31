from __future__ import annotations

import csv
import json
from datetime import datetime
from pathlib import Path
from typing import Any


def extract_dude_summary(results_directory: str | Path) -> dict[str, Any]:
    target = Path(results_directory)

    try:
        entries = list(target.iterdir())
    except Exception:
        return _create_summary_payload(
            internal_files=[],
            external_files=[],
            files_with_internal_duplication=0,
            internal_duplicated_lines_total=0,
            external_pairs_total=0,
            external_duplicated_lines_total=0,
            unique_external_files_count=0,
            top_internal_files=[],
            top_external_pairs=[],
            has_data_quality_issues=True,
        )

    internal_files = sorted(
        [entry for entry in entries if entry.is_file() and entry.name.endswith('-internal_duplication.json')],
        key=lambda value: value.name,
    )
    external_files = sorted(
        [entry for entry in entries if entry.is_file() and entry.name.endswith('-external_duplication.csv')],
        key=lambda value: value.name,
    )

    internal_by_file: dict[str, int] = {}
    pair_lengths: dict[tuple[str, str], int] = {}
    unique_external_files: set[str] = set()

    has_parse_failures = False
    invalid_rows_total = 0
    external_pairs_total = 0
    external_duplicated_lines_total = 0

    for internal_file in internal_files:
        parsed = _parse_internal_duplication(internal_file)
        has_parse_failures = has_parse_failures or parsed['hadParseFailure']
        invalid_rows_total += parsed['invalidRows']

        for file_name, duplicated_lines in parsed['internalByFile'].items():
            internal_by_file[file_name] = internal_by_file.get(file_name, 0) + duplicated_lines

    for external_file in external_files:
        parsed = _parse_external_duplication(external_file)
        has_parse_failures = has_parse_failures or parsed['hadParseFailure']
        invalid_rows_total += parsed['invalidRows']
        external_pairs_total += parsed['pairsTotal']
        external_duplicated_lines_total += parsed['duplicatedLinesTotal']
        unique_external_files.update(parsed['uniqueFiles'])

        for pair_key, duplicated_lines in parsed['pairLengths'].items():
            pair_lengths[pair_key] = pair_lengths.get(pair_key, 0) + duplicated_lines

    files_with_internal_duplication = len(internal_by_file)
    internal_duplicated_lines_total = sum(internal_by_file.values())

    top_internal_files = _build_top_internal_files(internal_by_file)
    top_external_pairs = _build_top_external_pairs(pair_lengths)

    has_data_quality_issues = has_parse_failures or invalid_rows_total > 0

    return _create_summary_payload(
        internal_files=internal_files,
        external_files=external_files,
        files_with_internal_duplication=files_with_internal_duplication,
        internal_duplicated_lines_total=internal_duplicated_lines_total,
        external_pairs_total=external_pairs_total,
        external_duplicated_lines_total=external_duplicated_lines_total,
        unique_external_files_count=len(unique_external_files),
        top_internal_files=top_internal_files,
        top_external_pairs=top_external_pairs,
        has_data_quality_issues=has_data_quality_issues,
    )


def _parse_internal_duplication(file_path: Path) -> dict[str, Any]:
    internal_by_file: dict[str, int] = {}
    invalid_rows = 0
    had_parse_failure = False

    try:
        raw = json.loads(file_path.read_text(encoding='utf-8', errors='replace'))
        if not isinstance(raw, list):
            return {
                'internalByFile': {},
                'invalidRows': 0,
                'hadParseFailure': True,
            }

        for item in raw:
            if not isinstance(item, dict):
                invalid_rows += 1
                continue

            file_name = str(item.get('file') or '').strip()
            if not file_name:
                invalid_rows += 1
                continue

            try:
                duplicated_lines = int(item.get('value', 0))
            except Exception:
                invalid_rows += 1
                continue

            if duplicated_lines <= 0:
                continue

            internal_by_file[file_name] = internal_by_file.get(file_name, 0) + duplicated_lines
    except Exception:
        had_parse_failure = True

    return {
        'internalByFile': internal_by_file,
        'invalidRows': invalid_rows,
        'hadParseFailure': had_parse_failure,
    }


def _parse_external_duplication(file_path: Path) -> dict[str, Any]:
    invalid_rows = 0
    had_parse_failure = False
    pairs_total = 0
    duplicated_lines_total = 0
    unique_files: set[str] = set()
    pair_lengths: dict[tuple[str, str], int] = {}

    try:
        with file_path.open('r', encoding='utf-8', errors='replace', newline='') as handle:
            reader = csv.reader(handle)
            for row in reader:
                if len(row) < 3:
                    invalid_rows += 1
                    continue

                file_one = row[0].strip()
                file_two = row[1].strip()
                if not file_one or not file_two:
                    invalid_rows += 1
                    continue

                try:
                    duplicated_lines = int(row[2])
                except Exception:
                    invalid_rows += 1
                    continue

                if duplicated_lines <= 0:
                    continue

                pairs_total += 1
                duplicated_lines_total += duplicated_lines
                unique_files.add(file_one)
                unique_files.add(file_two)

                ordered_pair = tuple(sorted((file_one, file_two)))
                pair_lengths[ordered_pair] = pair_lengths.get(ordered_pair, 0) + duplicated_lines
    except Exception:
        had_parse_failure = True

    return {
        'invalidRows': invalid_rows,
        'hadParseFailure': had_parse_failure,
        'pairsTotal': pairs_total,
        'duplicatedLinesTotal': duplicated_lines_total,
        'uniqueFiles': unique_files,
        'pairLengths': pair_lengths,
    }


def _build_top_internal_files(internal_by_file: dict[str, int]) -> list[dict[str, Any]]:
    rows = [
        {
            'file': file_name,
            'duplicatedLines': duplicated_lines,
            'duplicatedLinesFormatted': _format_int(duplicated_lines),
        }
        for file_name, duplicated_lines in internal_by_file.items()
    ]
    rows.sort(key=lambda row: (-int(row['duplicatedLines']), str(row['file']).lower()))
    return rows


def _build_top_external_pairs(pair_lengths: dict[tuple[str, str], int]) -> list[dict[str, Any]]:
    rows = [
        {
            'fileOne': pair[0],
            'fileTwo': pair[1],
            'duplicatedLines': duplicated_lines,
            'duplicatedLinesFormatted': _format_int(duplicated_lines),
        }
        for pair, duplicated_lines in pair_lengths.items()
    ]
    rows.sort(
        key=lambda row: (
            -int(row['duplicatedLines']),
            str(row['fileOne']).lower(),
            str(row['fileTwo']).lower(),
        )
    )
    return rows


def _create_summary_payload(
    internal_files: list[Path],
    external_files: list[Path],
    files_with_internal_duplication: int,
    internal_duplicated_lines_total: int,
    external_pairs_total: int,
    external_duplicated_lines_total: int,
    unique_external_files_count: int,
    top_internal_files: list[dict[str, Any]],
    top_external_pairs: list[dict[str, Any]],
    has_data_quality_issues: bool,
) -> dict[str, Any]:
    generated_at = _iso_now()
    status = _resolve_status(
        internal_file_count=len(internal_files),
        external_file_count=len(external_files),
        has_data_quality_issues=has_data_quality_issues,
    )

    metadata = {
        'internal.files.input': len(internal_files),
        'external.files.input': len(external_files),
        'internal.files.with.duplication': files_with_internal_duplication,
        'internal.duplicated.lines.total': internal_duplicated_lines_total,
        'external.pairs.total': external_pairs_total,
        'external.duplicated.lines.total': external_duplicated_lines_total,
        'external.files.unique': unique_external_files_count,
        'generated.at': generated_at,
    }

    top_internal_preview = top_internal_files[:10]
    top_external_preview = top_external_pairs[:10]

    markdown_lines = [
        '## DuDe',
        '',
        f'- Internal duplication files: {_format_int(files_with_internal_duplication)}',
        f'- Internal duplicated lines (total): {_format_int(internal_duplicated_lines_total)}',
        f'- External duplication pairs: {_format_int(external_pairs_total)}',
        f'- External duplicated lines (total): {_format_int(external_duplicated_lines_total)}',
        f'- Unique files in external duplication: {_format_int(unique_external_files_count)}',
        '',
        '### Top Internal Duplication Files',
        '',
        '| File | Duplicated Lines |',
        '| --- | ---: |',
    ]

    if len(top_internal_preview) == 0:
        markdown_lines.append('| _none_ | 0 |')
    else:
        for row in top_internal_preview:
            markdown_lines.append(f"| {row['file']} | {row['duplicatedLinesFormatted']} |")

    markdown_lines.extend([
        '',
        '### Top External Duplication Pairs',
        '',
        '| File 1 | File 2 | Duplicated Lines |',
        '| --- | --- | ---: |',
    ])

    if len(top_external_preview) == 0:
        markdown_lines.append('| _none_ | _none_ | 0 |')
    else:
        for row in top_external_preview:
            markdown_lines.append(
                f"| {row['fileOne']} | {row['fileTwo']} | {row['duplicatedLinesFormatted']} |"
            )

    template_model = {
        'generatedAt': generated_at,
        'metrics': {
            'internalFilesInputFormatted': _format_int(len(internal_files)),
            'externalFilesInputFormatted': _format_int(len(external_files)),
            'filesWithInternalDuplicationFormatted': _format_int(files_with_internal_duplication),
            'internalDuplicatedLinesTotalFormatted': _format_int(internal_duplicated_lines_total),
            'externalPairsTotalFormatted': _format_int(external_pairs_total),
            'externalDuplicatedLinesTotalFormatted': _format_int(external_duplicated_lines_total),
            'uniqueExternalFilesCountFormatted': _format_int(unique_external_files_count),
        },
        'topInternalFiles': top_internal_preview,
        'topExternalPairs': top_external_preview,
    }

    return {
        'tool': 'dude',
        'status': status,
        'metadata': metadata,
        'markdown': '\n'.join(markdown_lines),
        'templateModel': template_model,
    }


def _resolve_status(internal_file_count: int, external_file_count: int, has_data_quality_issues: bool) -> str:
    if internal_file_count == 0 and external_file_count == 0:
        return 'failed'
    if has_data_quality_issues:
        return 'partial'
    return 'success'


def _format_int(value: int) -> str:
    return f'{value:,}'


def _iso_now() -> str:
    local_now = datetime.now().astimezone()
    return f"{local_now.strftime('%Y-%m-%d %H:%M:%S')} {_format_gmt_offset(local_now.strftime('%z'))}"


def _format_gmt_offset(offset: str) -> str:
    if len(offset) != 5:
        return 'GMT+0'

    sign = offset[0]
    hours = int(offset[1:3])
    minutes = int(offset[3:5])

    if minutes == 0:
        return f'GMT{sign}{hours}'

    return f'GMT{sign}{hours}:{minutes:02d}'
