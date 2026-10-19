#!/usr/bin/env python3
"""Static integrity checks. These are not an Android compiler, Lint, or a runtime test."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
main=root/'app/src/main'
resources={};ids=set();parsed=0
android='{http://schemas.android.com/apk/res/android}'
for file in main.rglob('*.xml'):
    tree=ET.parse(file);parsed+=1
    if file.parent.name=='values':
        for child in tree.getroot():
            if child.get('name'):resources.setdefault(child.tag,set()).add(child.get('name'))
    elif file.parent.name.split('-')[0] in ('drawable','mipmap','layout','xml'):
        resources.setdefault(file.parent.name.split('-')[0],set()).add(file.stem)
    for element in tree.iter():
        value=element.get(android+'id','')
        if value.startswith('@+id/'):ids.add(value.split('/')[1])
resources['id']=ids
missing=[]
for file in main.rglob('*'):
    if file.suffix not in ('.java','.xml'):continue
    text=file.read_text()
    if file.suffix=='.java':refs=re.findall(r'(?<![\w.])R\.(\w+)\.(\w+)',text)
    else:refs=re.findall(r'@(?!(?:android):)(\w+)/([\w.]+)',text)
    for kind,name in refs:
        if kind=='null':continue
        if name not in resources.get(kind,set()):missing.append((str(file.relative_to(root)),kind,name))
assert not missing,missing
manifest=ET.parse(main/'AndroidManifest.xml').getroot()
permissions={el.get(android+'name') for el in manifest.findall('uses-permission')}
for forbidden in ('android.permission.INTERNET','android.permission.CAMERA','android.permission.RECORD_AUDIO','android.permission.READ_CONTACTS','android.permission.SYSTEM_ALERT_WINDOW'):
    assert forbidden not in permissions,forbidden
application=manifest.find('application')
for el in application:
    if el.tag=='service':
        assert el.get(android+'exported')=='false'
        assert el.get(android+'foregroundServiceType')=='specialUse'
for file in main.rglob('*.java'):
    content=file.read_text()
    for forbidden in ('Shizuku.newProcess','Runtime.getRuntime().exec','/sys/','TODO','FIXME'):
        assert forbidden not in content,(file.name,forbidden)
aidl=(main/'aidl/com/everyonesniche/nightkeys/bridge/IPrivilegedBridge.aidl').read_text()
methods=re.findall(r'\w+\s+\w+\([^;]*?\)\s*(?:=\s*(\d+))?\s*;',aidl)
assert len(methods)==6 and all(methods),'Every AIDL method must have an explicit transaction id'
assert len(set(methods))==6 and '16777114' in methods
print(f'PASS: {parsed} XML files parsed; all local resource references resolved.')
print('PASS: source manifest permissions, service exposure, AIDL transaction ids and forbidden-pattern checks.')
print('NOT RUN: AAPT2, Android Lint, dependency resolution, Android Java type checking, APK packaging or device testing.')
