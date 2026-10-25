"""Prepare an APK and updater manifest from the actual compiled package metadata.

Example: python tools/prepare_release.py --apk app/build/outputs/apk/debug/app-debug.apk
  --aapt <Android SDK>/build-tools/36.0.0/aapt --out <release-directory>
Always sign with the same private keystore used by installed TerraNav versions.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--aapt', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as archive:
        if archive.testzip() is not None:
            raise ValueError('Corrupt APK')
    badging = subprocess.check_output([str(args.aapt), 'dump', 'badging', str(args.apk)], encoding='utf-8')
    package = re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging)
    minimum = re.search(r"sdkVersion:'([0-9]+)'", badging)
    if not package or not minimum or package[1] != 'br.tec.ipsol.terranav.test':
        raise ValueError('Unexpected application package')
    if not re.fullmatch(r'[0-9A-Za-z._-]+', package[3]):
        raise ValueError('Invalid version name')
    args.out.mkdir(parents=True, exist_ok=True)
    filename = f'TerraNav-{package[3]}.apk'
    target = args.out / filename
    if args.apk.resolve() != target.resolve():
        shutil.copy2(args.apk, target)
    manifest = dict(applicationId=package[1], versionCode=int(package[2]), versionName=package[3],
                    apkName=filename, minSdk=int(minimum[1]), size=target.stat().st_size,
                    sha256=hashlib.sha256(target.read_bytes()).hexdigest())
    (args.out / 'terranav-update.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(manifest, indent=2))

if __name__ == '__main__':
    main()
