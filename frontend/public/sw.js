// NFR-07 (PWA-installierbar): Chrome verlangt fuer den Installations-Prompt einen registrierten
// Service Worker MIT fetch-Handler, unabhaengig davon ob er tatsaechlich etwas cached.
// Bewusst OHNE Caching-Strategie: Ernaehrungs-/Trainingsdaten muessen immer frisch vom Server
// kommen (Offline-Logging selbst laeuft ueber die eigene client_id-Warteschlange, siehe
// offlineQueue.ts -- ein Service-Worker-Cache wuerde dem hier nur in die Quere kommen, z.B. durch
// veraltete API-Antworten). Reiner Passthrough also identisch zum Verhalten ohne Service Worker.
self.addEventListener("fetch", () => {});
