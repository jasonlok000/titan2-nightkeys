#!/usr/bin/env python3
"""Attach a PUBLIC apksigner-produced block to its exact CI input.

This is not a signer: it holds no keystore, cannot authorize other APK contents,
and refuses any input/output digest mismatch. Android's official apksigner must
independently verify the resulting APK and pinned certificate before publication.
"""
from pathlib import Path
import argparse, base64, hashlib, json, re, struct, zipfile, zlib

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('request', type=Path)
p.add_argument('unsigned_apk', type=Path)
p.add_argument('output_apk', type=Path)
p.add_argument('--certificate-pin', type=Path, required=True)
a=p.parse_args()
r=json.loads(a.request.read_text())
assert r['format']==1
assert re.fullmatch(r'\d+\.\d+\.\d+(?:-[a-z]+\.\d+)?',r['version'])
assert re.fullmatch(r'[0-9a-f]{40}',r['sourceSha'])
assert r['certificateSha256']==a.certificate_pin.read_text().strip()
assert 0 <= r['paddingLength'] < 4096
u=a.unsigned_apk.read_bytes()
assert hashlib.sha256(u).hexdigest()==r['unsignedSha256'],'Unsigned CI APK differs'
assert len(r['signingBlockZlibBase64'])<65536
compressed=base64.b64decode(r['signingBlockZlibBase64'],validate=True)
d=zlib.decompressobj();block=d.decompress(compressed,65537)
assert d.eof and not d.unused_data and not d.unconsumed_tail and 32<len(block)<=65536
assert block[-16:]==b'APK Sig Block 42'
assert struct.unpack_from('<Q',block,0)[0]==len(block)-8
assert struct.unpack_from('<Q',block,len(block)-24)[0]==len(block)-8
e=u.rfind(b'PK\x05\x06')
assert e>=0 and e+22+struct.unpack_from('<H',u,e+20)[0]==len(u)
assert struct.unpack_from('<HH',u,e+4)==(0,0)
cd=struct.unpack_from('<I',u,e+16)[0]
assert cd<e and u[cd:cd+4]==b'PK\x01\x02'
assert u[cd-16:cd]!=b'APK Sig Block 42','Expected unsigned APK only'
extra=bytes(r['paddingLength'])+block
tail=bytearray(u[cd:]);struct.pack_into('<I',tail,e-cd+16,cd+len(extra))
result=u[:cd]+extra+tail
assert hashlib.sha256(result).hexdigest()==r['apkSha256'],'Public signature does not match expected final APK'
a.output_apk.parent.mkdir(parents=True,exist_ok=True)
a.output_apk.write_bytes(result)
with zipfile.ZipFile(a.output_apk) as z:assert z.testzip() is None
print('Exact offline-signed APK reconstructed:',r['apkSha256'])
