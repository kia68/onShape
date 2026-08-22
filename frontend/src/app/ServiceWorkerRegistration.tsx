"use client";

import { useEffect } from "react";

/** NFR-07: registriert den Passthrough-Service-Worker (siehe `public/sw.js`) -- noetig, damit
 * Chrome den "Zum Startbildschirm hinzufuegen"-Installations-Prompt ueberhaupt anbietet. */
export function ServiceWorkerRegistration() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {});
    }
  }, []);
  return null;
}
