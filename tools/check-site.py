"""Dependency-free static link, catalogue and release-consistency checks."""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit, unquote
import json, re
root=Path(__file__).resolve().parents[1]
site=root/'site'
class Page(HTMLParser):
    def __init__(self,text):
        super().__init__(); self.ids=set(); self.links=[]; self.scripts=[]; self.feed(text)
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if 'id' in a:
            assert a['id'] not in self.ids, 'Duplicate id '+a['id']
            self.ids.add(a['id'])
        for key in ('href','src'):
            if key in a:self.links.append(a[key])
        if tag=='script' and 'src' in a:self.scripts.append(a['src'])
pages={p.relative_to(site).as_posix():Page(p.read_text()) for p in site.glob('*.html')}
for name,p in pages.items():
    for url in p.links:
        u=urlsplit(url)
        if u.scheme or u.netloc:continue
        path=unquote(u.path)
        if path.startswith('/titan2-nightkeys/'):path=path[len('/titan2-nightkeys/'):]
        if path in ('','./'):path=name if u.fragment and u.path=='' else 'index.html'
        target=site/path
        if target.is_dir():target=target/'index.html'
        assert target.is_file(),f'{name}: missing {url}'
        if u.fragment:
            parsed=pages.get(target.relative_to(site).as_posix())
            assert parsed and u.fragment in parsed.ids,f'{name}: missing anchor {url}'
    assert all(not urlsplit(s).netloc for s in p.scripts),'External script in '+name
catalog=json.loads((site/'data/tools.json').read_text())
assert catalog['schemaVersion']==1
ids=set()
for tool in catalog['tools']:
    assert re.fullmatch('[a-z0-9-]+',tool['id']) and tool['id'] not in ids
    ids.add(tool['id']); u=urlsplit(tool['source'])
    assert u.scheme=='https' and u.netloc=='github.com'
    assert all(isinstance(tool[k],str) and tool[k] for k in ('name','description','status','guide','platform'))
release=json.loads((site/'data/release.json').read_text())
version=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)
assert release['version']==version['versionName']
assert release['tag']=='v'+release['version']
assert f"NightKeys-{release['version']}.apk" in (site/'setup.html').read_text()
print(f'PASS: {len(pages)} static pages; internal links, anchors, catalogue, local scripts and release identity.')
