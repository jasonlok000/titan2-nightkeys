"""Real browser navigation/interaction checks. Requires playwright==1.57.0."""
from pathlib import Path
from functools import partial
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from threading import Thread
from playwright.sync_api import sync_playwright
import json, os
root=Path(__file__).resolve().parents[1]
output=root/'build/site-evidence';output.mkdir(parents=True,exist_ok=True)
server=ThreadingHTTPServer(('127.0.0.1',8765),partial(SimpleHTTPRequestHandler,directory=str(root/'site')))
Thread(target=server.serve_forever,daemon=True).start()
results=[]
# Titan 2 Elite screens are 1080 × 1200 (9:10); 360×400 and 432×480 are CSS viewports at that ratio.
viewports=[('desktop',1440,1000),('mobile',390,844),('small',320,700),('titan',360,400),('titan-large',432,480),('titan-short',360,330)]
try:
    with sync_playwright() as p:
        browser=p.chromium.launch(headless=True, executable_path=os.environ.get('PLAYWRIGHT_CHROMIUM_EXECUTABLE'))
        for label,w,h in viewports:
            context=browser.new_context(viewport={'width':w,'height':h},reduced_motion='reduce')
            page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
            page.goto('http://127.0.0.1:8765/',wait_until='networkidle')
            assert page.title()=='Titan Tools — NightKeys for the Titan 2 Elite'
            assert page.locator('h1').is_visible()
            assert page.locator('.elite-key-row').count()==4
            assert page.locator('[data-row="1"] .elite-key').count()==10
            assert page.locator('[data-row="2"] .elite-key').count()==10
            assert page.locator('[data-row="3"] .elite-key').count()==10
            assert page.locator('[data-row="4"] .elite-key').count()==7
            demo=page.locator('#demo');phone=page.locator('[data-phone]')
            assert phone.get_attribute('data-lit')=='true' and demo.get_attribute('data-room')=='dark'
            assert page.get_by_role('button',name='Screen off').count()==0
            # Reduced motion: no automatic room changes.
            assert page.locator('#demo-auto').inner_text()=='Play auto-switch'
            page.get_by_role('button',name='Bright room',exact=True).click();page.wait_for_timeout(350)
            assert phone.get_attribute('data-lit')=='false' and demo.get_attribute('data-room')=='bright'
            assert page.locator('#demo-status').inner_text()=='Bright room · keyboard light off'
            page.get_by_role('button',name='Dark room',exact=True).click()
            assert phone.get_attribute('data-lit')=='true' and demo.get_attribute('data-room')=='dark'
            assert page.locator('#demo-status').inner_text()=='Dark room · keyboard light on'
            demo.scroll_into_view_if_needed()
            assert demo.bounding_box()['height']<=h or w>760, f'{label}: demo taller than the screen'
            for button in page.locator('.demo-preset button').all():
                assert button.bounding_box()['height']>=40
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            page.screenshot(path=str(output/f'home-{label}.png'),full_page=True)
            demo.screenshot(path=str(output/f'demo-dark-{label}.png'))
            page.get_by_role('button',name='Bright room',exact=True).click();page.wait_for_timeout(350)
            demo.screenshot(path=str(output/f'demo-bright-{label}.png'))
            page.goto('http://127.0.0.1:8765/setup.html',wait_until='networkidle')
            assert page.locator('.step').count()==3
            assert page.locator('.step h2').all_text_contents()==['Get Shizuku, the helper app','Connect Shizuku to NightKeys','Turn on NightKeys']
            assert page.locator('#connect .part').count()==4
            assert page.locator('#restart').is_visible()
            page.locator('[data-step-check="shizuku"]').check()
            assert page.locator('#setup-progress').inner_text()=='1 of 3 steps complete'
            page.reload(wait_until='networkidle')
            assert page.locator('[data-step-check="shizuku"]').is_checked()
            page.locator('#reset-progress').click()
            assert not page.locator('[data-step-check="shizuku"]').is_checked()
            page.get_by_role('link',name='Next: step 2 ↓').click()
            assert page.evaluate("location.hash")=='#connect'
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            page.screenshot(path=str(output/f'setup-{label}.png'),full_page=True)
            for route in ('contribute.html','privacy.html'):
                page.goto('http://127.0.0.1:8765/'+route,wait_until='networkidle')
                assert page.locator('h1').is_visible()
                assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            assert not errors,errors
            results.append({'viewport':label,'size':[w,h],'demo':'pass','navigation':'pass','checklistPersistence':'pass','overflow':False,'pageErrors':errors})
            context.close()
        # With motion allowed, the demo tours between rooms until paused.
        context=browser.new_context(viewport={'width':1440,'height':1000})
        page=context.new_page();page.goto('http://127.0.0.1:8765/#demo',wait_until='networkidle')
        page.wait_for_function("document.querySelector('#demo').dataset.room==='bright'",timeout=6000)
        page.locator('#demo-auto').click()
        assert page.locator('#demo-auto').inner_text()=='Play auto-switch'
        room=page.locator('#demo').get_attribute('data-room');page.wait_for_timeout(4000)
        assert page.locator('#demo').get_attribute('data-room')==room
        context.close()
        context=browser.new_context(java_script_enabled=False,viewport={'width':390,'height':844})
        page=context.new_page();page.goto('http://127.0.0.1:8765/setup.html')
        assert page.locator('.step').count()==3 and page.get_by_role('link',name='Download NightKeys',exact=True).count()==1
        page.goto('http://127.0.0.1:8765/')
        assert page.locator('[data-phone]').is_visible() and not page.get_by_role('button',name='Bright room').is_visible()
        context.close();browser.close()
finally:server.shutdown()
(output/'results.json').write_text(json.dumps({'checks':results,'autoSwitch':'pass','noJavaScriptGuide':'pass'},indent=2))
print(f'PASS: {len(viewports)} viewports incl. Titan 9:10, real navigation, room demo, auto-switch, checklist persistence/reset, no-JS pages.')
