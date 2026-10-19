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
try:
    with sync_playwright() as p:
        browser=p.chromium.launch(headless=True)
        for label,w,h in [('desktop',1440,1000),('mobile',390,844),('small',320,700)]:
            context=browser.new_context(viewport={'width':w,'height':h})
            page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
            page.goto('http://127.0.0.1:8765/',wait_until='networkidle')
            assert page.title()=='Titan Tools — NightKeys for the Titan 2 Elite'
            assert page.locator('h1').is_visible()
            assert page.locator('[data-phone]').get_attribute('data-lit')=='true'
            page.get_by_role('button',name='Bright room',exact=True).click();page.wait_for_timeout(350)
            assert page.locator('[data-phone]').get_attribute('data-lit')=='false'
            page.get_by_role('button',name='Dark room',exact=True).click()
            assert page.locator('[data-phone]').get_attribute('data-lit')=='true'
            page.get_by_role('button',name='Screen off',exact=True).click()
            assert page.locator('[data-phone]').get_attribute('data-lit')=='false'
            assert page.locator('[data-phone]').get_attribute('data-screen')=='false'
            page.get_by_role('button',name='Dark room',exact=True).click()
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            page.screenshot(path=str(output/f'home-{label}.png'),full_page=True)
            page.get_by_role('link',name='Walk me through setup').click()
            assert page.locator('.step').count()==8
            page.locator('[data-step-check="phone"]').check()
            assert page.locator('#setup-progress').inner_text()=='1 of 8 steps complete'
            page.reload(wait_until='networkidle')
            assert page.locator('[data-step-check="phone"]').is_checked()
            page.locator('#reset-progress').click()
            assert not page.locator('[data-step-check="phone"]').is_checked()
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            page.screenshot(path=str(output/f'setup-{label}.png'),full_page=True)
            for route in ('contribute.html','privacy.html'):
                page.goto('http://127.0.0.1:8765/'+route,wait_until='networkidle')
                assert page.locator('h1').is_visible()
                assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
            assert not errors,errors
            results.append({'viewport':label,'size':[w,h],'demo':'pass','navigation':'pass','checklistPersistence':'pass','overflow':False,'pageErrors':errors})
            context.close()
        context=browser.new_context(java_script_enabled=False,viewport={'width':390,'height':844})
        page=context.new_page();page.goto('http://127.0.0.1:8765/setup.html')
        assert page.locator('.step').count()==8 and page.get_by_role('link',name='Download NightKeys APK').count()==1
        context.close();browser.close()
finally:server.shutdown()
(output/'results.json').write_text(json.dumps({'checks':results,'noJavaScriptGuide':'pass'},indent=2))
print('PASS: three viewports, real navigation, light demo, checklist persistence/reset, no-JS guide.')
