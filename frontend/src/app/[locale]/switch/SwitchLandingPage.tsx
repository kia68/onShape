import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";

/** BIZ-04 (§15.4 "Wechsler-Kampagne": "Landing Pages gezielt auf die Beschwerden ausgerichtet").
 * Bewusst NUR eine Beispielseite fuer die MyFitnessPal-Beschwerdegruppe (Barcode-Paywall,
 * §11-Vergleichstabelle) statt einer vollen Kampagnen-/SEO-Infrastruktur (eigene Landing-Pages
 * pro Quelle, Tracking, A/B-Tests) -- das braeuchte Marketing-Text-/Strategie-Entscheidungen
 * ausserhalb dieser Session, analog zu LEGAL-07 (Videoproduktion). Zeigt aber den vollstaendigen,
 * funktionierenden Mechanismus: Beschwerde -> Gegenargument -> Ein-Klick-Import (Epic
 * Integrationen, FR-153) -> Pricing. */
export function SwitchLandingPage() {
  const t = useTranslations("Switch");

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8 p-6">
      <div className="text-center">
        <h1 className="text-3xl font-semibold">{t("title")}</h1>
        <p className="mt-2 text-muted-foreground">{t("subtitle")}</p>
      </div>

      <div className="flex flex-col gap-3">
        <ComplaintLine complaint={t("complaints.barcodePaywall.complaint")} answer={t("complaints.barcodePaywall.answer")} />
        <ComplaintLine complaint={t("complaints.entryLimit.complaint")} answer={t("complaints.entryLimit.answer")} />
        <ComplaintLine complaint={t("complaints.ads.complaint")} answer={t("complaints.ads.answer")} />
      </div>

      <div className="rounded-md border border-input p-6 text-center">
        <h2 className="text-lg font-semibold">{t("importTitle")}</h2>
        <p className="mt-1 text-sm text-muted-foreground">{t("importDescription")}</p>
        <div className="mt-4 flex flex-col items-center gap-2 sm:flex-row sm:justify-center">
          <Link href="/import">
            <Button>{t("importCta")}</Button>
          </Link>
          <Link href="/pricing" className="text-sm text-muted-foreground underline underline-offset-4">
            {t("pricingLink")}
          </Link>
        </div>
      </div>
    </div>
  );
}

function ComplaintLine({ complaint, answer }: { complaint: string; answer: string }) {
  return (
    <div className="rounded-md border border-input p-4">
      <p className="text-sm text-muted-foreground line-through">{complaint}</p>
      <p className="mt-1 font-medium">{answer}</p>
    </div>
  );
}
