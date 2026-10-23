#!/usr/bin/env python3
"""Verify candidate signatures and compare APK contents with this run's unsigned builds."""
import hashlib, pathlib, re, subprocess, sys, zipfile
root=pathlib.Path(__file__).resolve().parents[1]
expected=(root/'distribution/certificate.sha256').read_text().strip()
jar=root/'unsigned/apksigner.jar'
pairs=[('DATACLIAR.apk','app-unsigned.apk'),('upgrade-base.apk','upgrade-base-unsigned.apk'),('instrumentation.apk','instrumentation-unsigned.apk')]

PROVENANCE = 'META-INF/version-control-info.textproto'
BUILD_INPUTS = ['app', 'fixture', 'build.gradle', 'settings.gradle',
                'gradle.properties', 'gradle', 'gradlew', 'gradlew.bat']

def normalized_entry(name, data):
    if name != PROVENANCE:
        return data
    # AGP embeds HEAD even when the only new commit supplies signed APKs.
    # Accept a different revision only if every tracked build input is identical.
    value = data.decode('utf-8')
    revisions = re.findall(r'revision: "([0-9a-f]{40})"', value)
    if len(revisions) != 1:
        raise RuntimeError('Unexpected APK provenance')
    revision = revisions[0]
    found = subprocess.run(['git', 'cat-file', '-e', revision + '^{commit}'],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if found.returncode:
        subprocess.run(['git', 'fetch', '--no-tags', '--depth=1', 'origin', revision], check=True)
    subprocess.run(['git', 'diff', '--exit-code', revision, 'HEAD', '--', *BUILD_INPUTS],
                   check=True, stdout=subprocess.DEVNULL)
    return value.replace(revision, '<verified-identical-build-inputs>').encode('utf-8')

def contents(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist()
        if len(names)!=len(set(names)):raise RuntimeError('Duplicate ZIP entries')
        return {n:hashlib.sha256(normalized_entry(n, archive.read(n))).hexdigest() for n in names if not re.match(r'^META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))$',n,re.I)}

if any(not (root/'distribution'/signed).is_file() for signed,_ in pairs):
    print('Unsigned candidate ready. A maintainer must sign all three APKs with the persistent private key.')
    sys.exit(2)
for signed,unsigned in pairs:
    path=root/'distribution'/signed
    result=subprocess.run(['java','-jar',str(jar),'verify','--verbose','--print-certs',str(path)],check=True,text=True,capture_output=True).stdout
    matches=re.findall(r'(?:Signer #\d+|V\d+(?:\.\d+)? Signer): certificate SHA-256 digest: ([0-9a-fA-F]+)',result)
    if set(matches)!={expected} or not re.search(r'Number of signers:\s*1\b',result):raise RuntimeError('Unexpected signer: '+signed)
    if contents(path)!=contents(root/'unsigned'/unsigned):raise RuntimeError('Signed candidate does not match this source build: '+signed)
    print('Verified certificate and build contents: '+signed)
