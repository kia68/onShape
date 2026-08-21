import { createNavigation } from "next-intl/navigation";
import { routing } from "./routing";

// Lokalisierte Varianten von Link/redirect/usePathname/useRouter, die
// automatisch das aktuelle Locale-Praefix (/de/... bzw. /en/...) beachten.
export const { Link, redirect, usePathname, useRouter, getPathname } =
  createNavigation(routing);
