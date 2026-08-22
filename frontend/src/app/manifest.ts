import type { MetadataRoute } from "next";

/**
 * NFR-07 (Mobile-first, PWA-installierbar). Next.js serviert diese Datei automatisch unter
 * `/manifest.webmanifest` und verlinkt sie im `<head>` -- kein manueller `<link rel="manifest">`
 * noetig. `icon.svg` ist ein funktionaler Platzhalter (Monogramm), kein finales Branding --
 * echtes App-Icon-Design ist ein eigenes, hier nicht enthaltenes Vorhaben.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "OnShape",
    short_name: "OnShape",
    description: "Ernaehrung, Training und Fortschritt an einem Ort.",
    start_url: "/",
    display: "standalone",
    background_color: "#0f172a",
    theme_color: "#0f172a",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
    ],
  };
}
