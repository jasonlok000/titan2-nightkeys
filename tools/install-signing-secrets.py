#!/usr/bin/env python3
"""Owner-only setup. Read a private signing backup and use authenticated gh CLI.
Never prints the keystore/passwords; never puts them in subprocess arguments.
"""
import argparse,base64,json,pathlib,subprocess,sys
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('private_backup',type=pathlib.Path)
a=p.parse_args()
repo='jasonlok000/titan2-nightkeys'
try:
    folder=a.private_backup.resolve()
    config=json.loads((folder/'signing.json').read_text())
    key=folder/'nightkeys-release.p12'
    if not key.is_file() or key.stat().st_size>100000:raise ValueError('Invalid keystore file')
    secrets={
      'NIGHTKEYS_KEYSTORE_BASE64':base64.b64encode(key.read_bytes()).decode(),
      'NIGHTKEYS_STORE_PASSWORD':config['storePassword'],
      'NIGHTKEYS_KEY_ALIAS':config['alias'],
      'NIGHTKEYS_KEY_PASSWORD':config['keyPassword']}
    if not all(isinstance(v,str) and v for v in secrets.values()):raise ValueError('Incomplete signing configuration')
    subprocess.run(['gh','auth','status'],check=True)
    confirm=input('Install the permanent signing secrets into '+repo+'? Type YES: ')
    if confirm!='YES':sys.exit('Cancelled; no secrets changed.')
    for name,value in secrets.items():
        subprocess.run(['gh','secret','set',name,'--repo',repo],input=value.encode(),check=True,
                       stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        print('Installed '+name)
    print('Done. Keep this private backup offline; do not commit or upload it.')
except (OSError,ValueError,KeyError,subprocess.CalledProcessError):
    sys.exit('Setup failed. Check the private backup files and GitHub CLI authorization. No secret values were printed.')
