#!/usr/bin/env python3
"""Check repository wiring and static configuration; never certify manual Pleos compliance."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / '.agents/skills/pleos-connect/references'

def require(condition, message):
    if not condition:
        raise ValueError(message)

def check_harness():
    for name in ['AGENTS.md', 'CLAUDE.md', 'docs/work/REPORT-FORMAT.md']:
        require((ROOT / name).is_file(), f'Missing {name}')
    require('@AGENTS.md' in (ROOT / 'CLAUDE.md').read_text(), 'Claude entrypoint missing AGENTS import')
    require((ROOT / '.claude/skills').resolve() == (ROOT / '.agents/skills').resolve(), 'Claude skills must share canonical source')
    pages = json.loads((REF / 'sources/index.json').read_text())
    hashes = {p['file'][:-3] for p in pages}
    require(len(hashes) == len(pages) and len(pages) > 0, 'Duplicate/empty page inventory')
    for page in pages:
        p = REF / 'sources' / page['file']
        require(p.is_file(), f'Missing source {p}')
        require(hashlib.sha256(p.read_bytes()).hexdigest() == page['sha256'], f'Source changed without index update: {p}')
        for asset in re.findall(r'^assets/[^\s]+', p.read_text(), re.M):
            require((p.parent / asset).is_file(), f'Missing diagram {asset}')
    rules = json.loads((REF / 'rules.json').read_text())
    ids = {r['id'] for r in rules}
    require(len(ids) == len(rules), 'Duplicate rule ID')
    for rule in rules:
        require(set(rule['documents']) <= hashes, f'Unknown source for {rule["id"]}')
    for skill in ['pleos-connect', 'pleos-design', 'pleos-vehicle', 'pleos-verify']:
        p = ROOT / '.agents/skills' / skill / 'SKILL.md'
        require(p.is_file(), f'Missing skill {skill}')
        content = p.read_text()
        require(content.startswith('---\n') and f'name: {skill}\n' in content, f'Invalid frontmatter {skill}')
    n = '{http://schemas.android.com/apk/res/android}'
    manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    features = {x.get(n+'name'): x.get(n+'required') for x in manifest.findall('uses-feature')}
    require(features.get('android.hardware.type.automotive') == 'true', 'Automotive must be required')
    require(features.get('android.hardware.touchscreen') == 'false', 'Touchscreen should not be required')
    require(any(x.get(n+'name') == '.MainActivity' for x in manifest.findall('.//activity')), 'General launcher Activity missing')
    print(f'PASS: harness wiring, {len(pages)} source pages, {len(rules)} rule gates, static Manifest')
    print('Scope: no UI, vehicle runtime, document freshness or App Market certification checked.')
    return ids, hashes

def check_report(path, ids, hashes):
    report = json.loads(path.read_text())
    require(isinstance(report.get('task'), str) and report['task'].strip(), 'Report task required')
    selected = report.get('selected_rules')
    require(isinstance(selected, list) and selected and set(selected) <= ids, 'Select known rule IDs')
    checks = report.get('checks')
    require(isinstance(checks, list) and checks, 'Report checks required')
    seen = set()
    statuses = []
    for check in checks:
        id = check.get('id', '')
        doc = re.fullmatch(r'DOC-([0-9a-f]{12})#(.+)', id)
        require(id in ids or (doc and doc[1] in hashes), f'Unknown check {id}')
        require(id not in seen, f'Duplicate check {id}')
        seen.add(id)
        status = check.get('status')
        require(status in ['PASS', 'FAIL', 'NOT_TESTED', 'N/A'], f'Invalid status {id}')
        evidence = check.get('evidence')
        require(isinstance(evidence, str) and evidence.strip(), f'Evidence/reason missing for {id}')
        statuses.append(status)
    require(set(selected) <= seen, 'Selected rule missing a check')
    require(any(x.startswith('DOC-') for x in seen), 'Add individual source requirements, not only gate summaries')
    code = 1 if 'FAIL' in statuses else 2 if 'NOT_TESTED' in statuses else 0
    print(f'Report: {report["task"]}; {len(checks)} checks; exit={code}')
    return code

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', action='store_true')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    try:
        ids, hashes = check_harness()
        if args.build:
            result = subprocess.run(['./gradlew', ':app:assembleDebug', ':app:lintDebug', '--console=plain'], cwd=ROOT)
            if result.returncode:
                return result.returncode
        return check_report(args.report, ids, hashes) if args.report else 0
    except (ValueError, OSError, KeyError, TypeError, ET.ParseError) as exc:
        print(f'FAIL: {exc}', file=sys.stderr)
        return 1

if __name__ == '__main__':
    sys.exit(main())
