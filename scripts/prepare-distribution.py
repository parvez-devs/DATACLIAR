#!/usr/bin/env python3
import hashlib,json,pathlib,shutil
r=pathlib.Path(__file__).resolve().parents[1]
v=dict(line.strip().split('=',1) for line in (r/'app/version.properties').read_text().splitlines() if '=' in line)
version=v['versionName'];tag='v'+version
out=r/'dist';out.mkdir(exist_ok=True)
name='DATACLIAR-'+version+'.apk';shutil.copy2(r/'distribution/DATACLIAR.apk',out/name)
sha=hashlib.sha256((out/name).read_bytes()).hexdigest()
metadata={'channel':'stable','packageName':'com.axiom.datacliar','versionCode':int(v['versionCode']),'versionName':version,'certificateSha256':(r/'distribution/certificate.sha256').read_text().strip(),'sha256':sha,'apkUrl':'https://github.com/parvez-devs/DATACLIAR/releases/download/'+tag+'/'+name}
(out/'update.json').write_text(json.dumps(metadata,indent=2)+'\n')
(out/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in [out/name,out/'update.json']))
print('Prepared signed release '+tag)
