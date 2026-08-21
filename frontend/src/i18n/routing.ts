import { defineRouting } from "next-intl/routing";

// KONZEPT.md §13: /de/... und /en/..., Spracherkennung ueber Accept-Language
// mit manueller Ueberschreibung (localePrefix "always" macht die gewaehlte
// Sprache in der URL sichtbar/teilbar statt sie nur per Cookie zu merken).
export const routing = defineRouting({
  locales: ["de", "en"],
  defaultLocale: "de",
  localePrefix: "always",
});

export type AppLocale = (typeof routing.locales)[number];
