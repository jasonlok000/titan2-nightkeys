"""The old Titan Tools / NightKeys site is obsolete (owner, 2026-11-25): every page redirects to TitanTools on the TitanKeys site."""
from pathlib import Path
site = Path(__file__).resolve().parents[1] / 'site'
target = 'https://everyones-niche.com/titankeys/#tools'
pages = sorted(site.glob('*.html'))
for page in pages:
    text = page.read_text(encoding='utf-8')
    assert f'url={target}' in text and f'location.replace("{target}")' in text, f'{page.name} does not redirect to {target}'
print(f'PASS: {len(pages)} pages redirect to {target}.')
