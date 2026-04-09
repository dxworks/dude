from __future__ import annotations

import csv
import json
from datetime import datetime
from pathlib import Path
from typing import Any


def extract_dude_summary(results_directory: str | Path) -> dict[str, Any]:
    target = Path(results_directory)
    extension_language_map = _load_extension_language_map(Path(__file__).resolve().parent / 'languages.yml')

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
    internal_files_by_technology: dict[str, int] = {}
    internal_lines_by_technology: dict[str, int] = {}
    external_by_technology_pair: dict[tuple[str, str], dict[str, Any]] = {}
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

        for row in parsed['rows']:
            file_one = str(row[0])
            file_two = str(row[1])
            duplicated_lines = int(row[2])

            technology_one = _resolve_technology(file_one, extension_language_map)
            technology_two = _resolve_technology(file_two, extension_language_map)
            normalized_pair = _normalize_technology_pair(technology_one, technology_two)

            if normalized_pair not in external_by_technology_pair:
                external_by_technology_pair[normalized_pair] = {
                    'technologyOne': normalized_pair[0],
                    'technologyTwo': normalized_pair[1],
                    'pairCount': 0,
                    'duplicatedLines': 0,
                }

            external_by_technology_pair[normalized_pair]['pairCount'] += 1
            external_by_technology_pair[normalized_pair]['duplicatedLines'] += duplicated_lines

    files_with_internal_duplication = len(internal_by_file)
    internal_duplicated_lines_total = sum(internal_by_file.values())

    for file_name, duplicated_lines in internal_by_file.items():
        technology = _resolve_technology(file_name, extension_language_map)
        internal_files_by_technology[technology] = internal_files_by_technology.get(technology, 0) + 1
        internal_lines_by_technology[technology] = internal_lines_by_technology.get(technology, 0) + duplicated_lines

    top_internal_technologies = _build_top_internal_technologies(
        internal_files_by_technology,
        internal_lines_by_technology,
        files_with_internal_duplication,
        internal_duplicated_lines_total,
    )
    top_external_pairs = _build_top_external_technology_pairs(
        external_by_technology_pair,
        external_pairs_total,
        external_duplicated_lines_total,
    )

    has_data_quality_issues = has_parse_failures or invalid_rows_total > 0

    return _create_summary_payload(
        internal_files=internal_files,
        external_files=external_files,
        files_with_internal_duplication=files_with_internal_duplication,
        internal_duplicated_lines_total=internal_duplicated_lines_total,
        external_pairs_total=external_pairs_total,
        external_duplicated_lines_total=external_duplicated_lines_total,
        unique_external_files_count=len(unique_external_files),
        top_internal_technologies=top_internal_technologies,
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
    rows: list[tuple[str, str, int]] = []

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

                rows.append((file_one, file_two, duplicated_lines))
    except Exception:
        had_parse_failure = True

    return {
        'invalidRows': invalid_rows,
        'hadParseFailure': had_parse_failure,
        'pairsTotal': pairs_total,
        'duplicatedLinesTotal': duplicated_lines_total,
        'uniqueFiles': unique_files,
        'rows': rows,
    }


def _build_top_internal_technologies(
    internal_files_by_technology: dict[str, int],
    internal_lines_by_technology: dict[str, int],
    files_with_internal_duplication_total: int,
    internal_duplicated_lines_total: int,
) -> list[dict[str, Any]]:
    rows = [
        {
            'technology': technology,
            'duplicatedFiles': duplicated_files,
            'duplicatedLines': internal_lines_by_technology.get(technology, 0),
        }
        for technology, duplicated_files in internal_files_by_technology.items()
    ]

    for row in rows:
        duplicated_files = int(row['duplicatedFiles'])
        duplicated_lines = int(row['duplicatedLines'])
        duplicated_files_formatted = _format_int(duplicated_files)
        duplicated_lines_formatted = _format_int(duplicated_lines)
        duplicated_files_percent_formatted = _format_percent(duplicated_files, files_with_internal_duplication_total)
        duplicated_lines_percent_formatted = _format_percent(duplicated_lines, internal_duplicated_lines_total)

        row['duplicatedFilesFormatted'] = duplicated_files_formatted
        row['duplicatedLinesFormatted'] = duplicated_lines_formatted
        row['duplicatedFilesPercentFormatted'] = duplicated_files_percent_formatted
        row['duplicatedLinesPercentFormatted'] = duplicated_lines_percent_formatted
        row['duplicatedFilesWithPercentFormatted'] = (
            f'{duplicated_files_formatted} ({duplicated_files_percent_formatted})'
        )
        row['duplicatedLinesWithPercentFormatted'] = (
            f'{duplicated_lines_formatted} ({duplicated_lines_percent_formatted})'
        )

    rows.sort(
        key=lambda row: (
            -int(row['duplicatedLines']),
            -int(row['duplicatedFiles']),
            str(row['technology']).lower(),
        )
    )
    return rows


def _build_top_external_technology_pairs(
    external_by_technology_pair: dict[tuple[str, str], dict[str, Any]],
    external_pairs_total: int,
    external_duplicated_lines_total: int,
) -> list[dict[str, Any]]:
    rows = [
        {
            'technologyOne': value['technologyOne'],
            'technologyTwo': value['technologyTwo'],
            'pairCount': value['pairCount'],
            'pairCountFormatted': _format_int(value['pairCount']),
            'pairCountPercentFormatted': _format_percent(value['pairCount'], external_pairs_total),
            'duplicatedLines': value['duplicatedLines'],
            'duplicatedLinesFormatted': _format_int(value['duplicatedLines']),
            'duplicatedLinesPercentFormatted': _format_percent(
                value['duplicatedLines'],
                external_duplicated_lines_total,
            ),
            'pairCountWithPercentFormatted': (
                f"{_format_int(value['pairCount'])} ({_format_percent(value['pairCount'], external_pairs_total)})"
            ),
            'duplicatedLinesWithPercentFormatted': (
                f"{_format_int(value['duplicatedLines'])} ({_format_percent(value['duplicatedLines'], external_duplicated_lines_total)})"
            ),
        }
        for value in external_by_technology_pair.values()
    ]
    rows.sort(
        key=lambda row: (
            -int(row['duplicatedLines']),
            -int(row['pairCount']),
            str(row['technologyOne']).lower(),
            str(row['technologyTwo']).lower(),
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
    top_internal_technologies: list[dict[str, Any]],
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

    markdown_lines = [
        '## DuDe',
        '',
        f'- Internal duplication files: {_format_int(files_with_internal_duplication)}',
        f'- Internal duplicated lines (total): {_format_int(internal_duplicated_lines_total)}',
        f'- External duplication pairs: {_format_int(external_pairs_total)}',
        f'- External duplicated lines (total): {_format_int(external_duplicated_lines_total)}',
        f'- Unique files in external duplication: {_format_int(unique_external_files_count)}',
        '',
        '### Internal Duplication Technologies',
        '',
        '| Technology | Duplicated Files | Duplicated Lines |',
        '| --- | ---: | ---: |',
    ]

    if len(top_internal_technologies) == 0:
        markdown_lines.append('| _none_ | 0 (0.00%) | 0 (0.00%) |')
    else:
        for row in top_internal_technologies:
            markdown_lines.append(
                f"| {row['technology']} | {row['duplicatedFilesWithPercentFormatted']} | {row['duplicatedLinesWithPercentFormatted']} |"
            )

    markdown_lines.extend([
        '',
        '### External Duplication Technology Pairs',
        '',
        '| Technology Pair | Duplicated Files | Duplicated Lines |',
        '| --- | ---: | ---: |',
    ])

    if len(top_external_pairs) == 0:
        markdown_lines.append('| _none_ | 0 (0.00%) | 0 (0.00%) |')
    else:
        for row in top_external_pairs:
            markdown_lines.append(
                f"| {row['technologyOne']}  ↔  {row['technologyTwo']} | {row['pairCountWithPercentFormatted']} | {row['duplicatedLinesWithPercentFormatted']} |"
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
        'topInternalTechnologies': top_internal_technologies,
        'topExternalPairs': top_external_pairs,
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


def _format_percent(part: int, whole: int) -> str:
    if whole <= 0:
        return '0.00%'
    return f'{(part * 100.0) / whole:.2f}%'


def _normalize_technology_pair(technology_one: str, technology_two: str) -> tuple[str, str]:
    if technology_one.lower() <= technology_two.lower():
        return technology_one, technology_two
    return technology_two, technology_one


def _resolve_technology(file_path: str, extension_language_map: dict[str, str]) -> str:
    extension = Path(file_path).suffix.lower()
    if extension in extension_language_map:
        return extension_language_map[extension]

    return 'Unknown'


def _load_extension_language_map(languages_file: Path) -> dict[str, str]:
    try:
        lines = languages_file.read_text(encoding='utf-8', errors='replace').splitlines()
    except Exception:
        return {}

    extension_language_map: dict[str, str] = {}
    current_language = ''
    in_extensions = False

    for raw_line in lines:
        line = raw_line.rstrip()
        stripped = line.strip()

        if not stripped or stripped.startswith('#'):
            continue

        if not line.startswith(' '):
            current_language = _parse_language_key(line)
            in_extensions = False
            continue

        if not current_language:
            continue

        if line.startswith('  extensions:'):
            in_extensions = True
            continue

        if line.startswith('  ') and not line.startswith('  - '):
            in_extensions = False

        if in_extensions and stripped.startswith('- '):
            extension = stripped[2:].strip().strip('"\'').lower()
            if extension.startswith('.') and len(extension) > 1 and extension not in extension_language_map:
                extension_language_map[extension] = current_language

    return extension_language_map


def _parse_language_key(line: str) -> str:
    if ':' not in line:
        return ''
    key = line.split(':', 1)[0].strip()
    if key.startswith('"') and key.endswith('"'):
        return key[1:-1]
    if key.startswith("'") and key.endswith("'"):
        return key[1:-1]
    return key


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
