#!/usr/bin/env python3
"""Verify candidate signatures and compare APK contents with this run's unsigned builds."""
import hashlib, pathlib, re, subprocess, sys, zipfile
root=pathlib.Path(__file__).resolve().parents[1]
expected=(root/'distribution/certificate.sha256').read_text().strip()
jar=root/'unsigned/apksigner.jar'
pairs=[('DATACLIAR.apk','app-unsigned.apk'),('upgrade-base.apk','upgrade-base-unsigned.apk'),('instrumentation.apk','instrumentation-unsigned.apk')]

def contents(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist()
        if len(names)!=len(set(names)):raise RuntimeError('Duplicate ZIP entries')
        return {n:hashlib.sha256(archive.read(n)).hexdigest() for n in names if not re.match(r'^META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))$',n,re.I)}

if any(not (root/'distribution'/signed).is_file() for signed,_ in pairs):
    print('Unsigned candidate ready. A maintainer must sign all three APKs with the persistent private key.')
    sys.exit(2)
for signed,unsigned in pairs:
    path=root/'distribution'/signed
    result=subprocess.run(['java','-jar',str(jar),'verify','--verbose','--print-certs',str(path)],check=True,text=True,capture_output=True).stdout
    matches=re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)',result)
    if matches!=[expected]:raise RuntimeError('Unexpected signer: '+signed)
    if contents(path)!=contents(root/'unsigned'/unsigned):raise RuntimeError('Signed candidate does not match this source build: '+signed)
    print('Verified certificate and build contents: '+signed)
