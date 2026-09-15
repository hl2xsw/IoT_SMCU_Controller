// IoT SMCU Service Worker with auto cache invalidation
const CACHE = "iot-smcu-v4";

importScripts('https://storage.googleapis.com/workbox-cdn/releases/5.1.2/workbox-sw.js');

self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.filter((name) => name !== CACHE).map((name) => caches.delete(name))
      );
    }).then(() => self.clients.claim())
  );
});

workbox.routing.registerRoute(
  new RegExp('/*'),
  new workbox.strategies.NetworkFirst({
    cacheName: CACHE
  })
);

