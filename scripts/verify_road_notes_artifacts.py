#!/usr/bin/env python3
"""고정 빌드의 Manifest/스키마/release APK 정적 검사. 앱을 실행하지 않는다."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
MAIN = 'com.jinkim.hyundaiAndGretel.MainActivity'


def check_activity_do(manifest):
    activities = [a for a in manifest.findall('./application/activity')
                  if a.get(ANDROID + 'name') in ('.MainActivity', 'MainActivity', MAIN)]
    assert len(activities) == 1, 'MainActivity가 단 하나여야 합니다'
    all_do = manifest.findall('.//meta-data[@' + ANDROID + 'name="distractionOptimized"]')
    own = activities[0].findall('./meta-data[@' + ANDROID + 'name="distractionOptimized"]')
    assert len(all_do) == len(own) == 1, 'DO는 MainActivity의 직접 자식에만 한 번 선언해야 합니다'
    assert own[0].get(ANDROID + 'value') == 'true', 'DO는 literal true여야 합니다'


def verify(root=ROOT):
    check_activity_do(ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot())
    sdk = Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
    analyzer = sdk / 'cmdline-tools/latest/bin/apkanalyzer'
    artifacts = {}
    for variant, apk_name in [('debug', 'app-debug.apk'), ('release', 'app-release-unsigned.apk')]:
        merged = root / f'app/build/intermediates/merged_manifests/{variant}/process{variant.title()}Manifest/AndroidManifest.xml'
        check_activity_do(ET.parse(merged).getroot())
        apk = root / f'app/build/outputs/apk/{variant}/{apk_name}'
        packaged = ET.fromstring(subprocess.check_output([str(analyzer), 'manifest', 'print', str(apk)]))
        check_activity_do(packaged)
        service = packaged.find('./application/service[@' + ANDROID + 'name="com.jinkim.hyundaiAndGretel.service.RoadNoteService"]')
        assert service is not None
        service_type = service.get(ANDROID + 'foregroundServiceType')
        assert service_type == '0x9' or set(service_type.split('|')) == {'location', 'dataSync'}
        permissions = {p.get(ANDROID + 'name') for p in packaged.findall('uses-permission')}
        assert {'pleos.car.permission.FUSED_LOCATION', 'android.permission.FOREGROUND_SERVICE_LOCATION', 'android.permission.FOREGROUND_SERVICE_DATA_SYNC'} <= permissions
        with zipfile.ZipFile(apk) as archive:
            for abi, machine in [('arm64-v8a', 183), ('armeabi-v7a', 40)]:
                elf = archive.read(f'lib/{abi}/libh3-java.so')
                assert elf[:4] == b'\x7fELF' and elf[5] == 1 and int.from_bytes(elf[18:20], 'little') == machine
            if variant == 'release':
                permissions = {p.get(ANDROID + 'name') for p in packaged.findall('uses-permission')}
                assert not {'android.permission.INTERNET', 'android.permission.ACCESS_MOCK_LOCATION'} & permissions
                assert packaged.find('application').get(ANDROID + 'networkSecurityConfig') is None
                dex = b''.join(archive.read(n) for n in archive.namelist() if n.endswith('.dex'))
                for value in ['AndroidDemoInput', 'AndroidDemoInputSource', 'DemoGeometry', 'StartGate', 'DemoMode', 'DemoPhase', '127.0.0.1:8088', '시연 도구']:
                    assert value.encode() not in dex, value
                assert not any('debug_network_security' in n for n in archive.namelist())
        artifacts[variant] = hashlib.sha256(apk.read_bytes()).hexdigest()
    schemas = sorted((root / 'app/schemas').glob('*/*.json'))
    assert [p.stem for p in schemas] == ['1']
    db = json.loads(schemas[-1].read_text())['database']
    entities = {e['tableName']: e for e in db['entities']}
    assert db['version'] == 1 and 'demo_state' in entities
    note = entities['road_notes']
    assert not next(f for f in note['fields'] if f['columnName'] == 'scenarioId').get('notNull', False)
    receipt = entities['note_receipts']
    assert receipt['primaryKey']['columnNames'] == ['noteId', 'recipientId']
    assert any(f['table'] == 'road_notes' and f['onDelete'] == 'CASCADE' for f in receipt['foreignKeys'])
    return artifacts


if __name__ == '__main__':
    print(json.dumps({'apk_sha256': verify(), 'runtime': 'NOT_TESTED'}, ensure_ascii=False, indent=2))
