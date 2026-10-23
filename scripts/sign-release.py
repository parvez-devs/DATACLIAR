#!/usr/bin/env python3
"""Run locally with private signing material. NEVER upload the key/password to this repo or CI artifacts."""
import argparse, os, pathlib, subprocess
p=argparse.ArgumentParser();p.add_argument('--candidate',required=True,type=pathlib.Path);p.add_argument('--key-directory',required=True,type=pathlib.Path);p.add_argument('--output',required=True,type=pathlib.Path);p.add_argument('--java',default='java');args=p.parse_args()
args.output.mkdir(parents=True,exist_ok=True)
env=dict(os.environ);env['DATACLIAR_SIGN_PASS']=(args.key_directory/'password.txt').read_text().strip()
for source,target in [('app-unsigned.apk','DATACLIAR.apk'),('upgrade-base-unsigned.apk','upgrade-base.apk'),('instrumentation-unsigned.apk','instrumentation.apk')]:
    subprocess.run([args.java,'-jar',str(args.candidate/'apksigner.jar'),'sign','--ks',str(args.key_directory/'signing.p12'),'--ks-key-alias','datacliar','--ks-pass','env:DATACLIAR_SIGN_PASS','--key-pass','env:DATACLIAR_SIGN_PASS','--out',str(args.output/target),str(args.candidate/source)],env=env,check=True)
    subprocess.run([args.java,'-jar',str(args.candidate/'apksigner.jar'),'verify','--verbose','--print-certs',str(args.output/target)],check=True)
print('Signed APKs created. Upload only these APKs; keep the entire key directory private.')
