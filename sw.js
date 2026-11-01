/* Exclusive PWA scope: /alborz-communication-academy/
 * Do not control the main site, canonical academy content, or other installed apps.
 */
const NAME='alborz-standalone-shell-v1-20261101';
const PREFIX='alborz-standalone-shell-';
const SCOPE=new URL(self.registration.scope);
const ASSETS=['./','./index.html','./manifest.webmanifest','./icon-192.png','./icon-512.png'];
self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const cache=await caches.open(NAME);
    await Promise.allSettled(ASSETS.map(path=>cache.add(new URL(path,SCOPE))));
    await self.skipWaiting();
  })());
});
self.addEventListener('activate',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await Promise.all(keys.filter(k=>k.startsWith(PREFIX)&&k!==NAME).map(k=>caches.delete(k)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch',event=>{
  const req=event.request;
  if(req.method!=='GET')return;
  const u=new URL(req.url);
  if(u.origin!==SCOPE.origin||!u.pathname.startsWith(SCOPE.pathname))return;
  event.respondWith((async()=>{
    const cache=await caches.open(NAME);
    try{
      const response=await fetch(req);
      if(response.ok)cache.put(req,response.clone()).catch(()=>{});
      return response;
    }catch(e){
      const hit=await cache.match(req);
      if(hit)return hit;
      if(req.mode==='navigate'){
        const shell=await cache.match(new URL('./index.html',SCOPE));
        if(shell)return shell;
      }
      throw e;
    }
  })());
});
