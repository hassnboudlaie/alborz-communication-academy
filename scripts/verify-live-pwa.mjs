import assert from 'node:assert/strict';
import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';

const ROOT='https://hassnboudlaie.github.io/alborz-communication-academy/';
const COURSE='https://hassnboudlaie.github.io/hasan-boudlaie/alborz-communication-skills/';
await mkdir('artifacts',{recursive:true});
const browser=await chromium.launch({headless:true,args:['--no-sandbox']});

async function run(viewport,label){
  const context=await browser.newContext({viewport,deviceScaleFactor:1,locale:'fa-IR'});
  const page=await context.newPage();
  const failures=[];
  page.on('pageerror',e=>failures.push(String(e.message||e)));
  try{
    await page.goto(ROOT,{waitUntil:'domcontentloaded',timeout:60000});
    assert.equal(new URL(page.url()).pathname,'/alborz-communication-academy/','Top-level app URL must remain distinct');
    const manifest=await page.evaluate(async()=>{
      const response=await fetch('./manifest.webmanifest',{cache:'no-store'});
      if(!response.ok)throw Error('Manifest unavailable: '+response.status);
      return response.json();
    });
    assert.equal(manifest.scope,'/alborz-communication-academy/');
    assert.match(manifest.id,/alborz-communication-standalone-20261101/);
    assert.equal(manifest.display,'standalone');
    assert.ok(manifest.icons.length>=2,'Installable icons missing');

    const iframe=page.locator('#academy');
    await iframe.waitFor({state:'attached',timeout:20000});
    await page.waitForFunction((expected)=> {
      const frame=document.querySelector('#academy');
      try{
        return frame?.contentWindow?.location.href.startsWith(expected)
          && frame.contentDocument?.body?.innerText?.includes('جلسات دوره');
      }catch(e){return false;}
    },COURSE,{timeout:50000});
    const home=await iframe.evaluate(el=>({
      title:el.contentDocument.title,
      href:el.contentWindow.location.href,
      body:el.contentDocument.body.innerText.slice(0,180)
    }));
    assert.match(home.href,/alborz-communication-skills/);
    await page.screenshot({path:'artifacts/'+label+'-home.png',fullPage:false});
    console.log(label,'Home academy visible in independent app',home);

    await page.goto(ROOT+'?m=2',{waitUntil:'domcontentloaded',timeout:60000});
    await page.waitForFunction(()=>{
      const frame=document.getElementById('academy');
      try{
        return frame?.contentWindow?.location.href.includes('session2-demo.html')
          && frame.contentDocument?.body?.innerText?.includes('جلسه دوم');
      }catch(e){return false;}
    },null,{timeout:50000});
    const session=await page.locator('#academy').evaluate(el=>({
      title:el.contentDocument.title,
      href:el.contentWindow.location.href,
      heading:el.contentDocument.body.innerText.slice(0,240)
    }));
    await page.screenshot({path:'artifacts/'+label+'-session2.png',fullPage:false});
    assert.equal(new URL(page.url()).pathname,'/alborz-communication-academy/');
    console.log(label,'Session 2 loaded within independent app',session);

    const reg=await page.evaluate(async()=>{
      await new Promise(resolve=>setTimeout(resolve,1500));
      const registrations=await navigator.serviceWorker.getRegistrations();
      return registrations.map(x=>x.scope);
    });
    assert.ok(reg.includes(ROOT),'Standalone service worker scope not registered');
    console.log(label,'PWA registrations',reg);
    if(failures.length)console.log(label,'Page JS warnings:',failures);
  }finally{await context.close();}
}

try{
 await run({width:1365,height:850},'desktop');
 await run({width:390,height:844},'mobile');
 console.log('PASS: Independent Alborz PWA wraps the canonical course at both desktop and mobile widths.');
}finally{await browser.close();}
