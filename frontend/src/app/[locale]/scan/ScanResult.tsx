"use client";

import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { cn } from "@/lib/utils";
import type { AlternativeProduct, BarcodeScanResult, ScoreReason } from "@/lib/barcodeApi";

/** FR-44: Ampel-Darstellung mit Klartext-Begruendung. Farbgrenzen willkuerlich, aber
 * gaengiger Ampel-Konvention folgend (>=70 gut, >=40 mittel, sonst eher nicht). */
function scoreBand(score: number): "good" | "medium" | "poor" {
  if (score >= 70) return "good";
  if (score >= 40) return "medium";
  return "poor";
}

const BAND_COLOR: Record<string, string> = {
  good: "border-green-600 bg-green-600/10 text-green-700 dark:text-green-400",
  medium: "border-amber-500 bg-amber-500/10 text-amber-700 dark:text-amber-400",
  poor: "border-destructive bg-destructive/10 text-destructive",
};

function ReasonLine({ reason }: { reason: ScoreReason }) {
  const t = useTranslations("Barcode.reasons");
  if (!t.has(reason.code)) return null;
  return <li className="text-sm">{t(reason.code, reason.params as Record<string, string | number>)}</li>;
}

export function ScanResult({ result }: { result: BarcodeScanResult & { found: true; product: NonNullable<BarcodeScanResult["product"]> } }) {
  const t = useTranslations("Barcode");

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold">{result.product.name}</h2>
        {result.product.brand && <p className="text-sm text-muted-foreground">{result.product.brand}</p>}
      </div>

      {/* BIZ-01 (§15.1): Free-Tier-Monatsdeckel fuer Fit-Score & Kaufberatung erreicht --
          Allergen-/Praeferenz-Warnungen bleiben davon unberuehrt (siehe unten). */}
      {result.fitScoreGated ? (
        <div className="rounded-md border border-amber-500/50 bg-amber-500/10 p-4 text-sm">
          <p className="font-medium">{t("fitScoreGated.title")}</p>
          <p className="mt-1 text-muted-foreground">{t("fitScoreGated.message")}</p>
          <Link href="/pricing" className="mt-2 inline-block underline underline-offset-4">
            {t("fitScoreGated.upgradeLink")}
          </Link>
        </div>
      ) : result.score !== null ? (
        <div className={cn("rounded-md border p-4", BAND_COLOR[scoreBand(result.score)])}>
          <p className="text-2xl font-bold">{t("score.outOf100", { score: result.score })}</p>
          <p className="text-sm font-medium">{t(`score.${scoreBand(result.score)}`)}</p>
        </div>
      ) : null}

      {result.allergenMatches.length > 0 && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
          {result.allergenMatches.map((allergen) => (
            <p key={allergen}>{t("allergenWarning", { allergen })}</p>
          ))}
        </div>
      )}
      {result.dietaryPreferenceConflict && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
          {t("dietaryConflict", { preference: result.dietaryPreferenceConflict })}
        </div>
      )}

      <div className="flex gap-2 text-xs text-muted-foreground">
        {result.product.novaGroup && <span className="rounded-full border border-input px-2 py-1">{t("nova", { group: result.product.novaGroup })}</span>}
        {result.product.nutriscore && <span className="rounded-full border border-input px-2 py-1">{t("nutriscore", { grade: result.product.nutriscore })}</span>}
      </div>

      {!result.fitScoreGated && (
        <ul className="flex flex-col gap-1 rounded-md border border-input p-3">
          {result.reasons.map((reason, index) => (
            <ReasonLine key={`${reason.code}-${index}`} reason={reason} />
          ))}
        </ul>
      )}

      {!result.fitScoreGated && (
        <div className="rounded-md border border-input p-3">
          <h3 className="mb-2 text-sm font-semibold">{t("alternatives.title")}</h3>
          {result.alternatives.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t("alternatives.none")}</p>
          ) : (
            <ul className="flex flex-col gap-2">
              {result.alternatives.map((alt) => (
                <AlternativeLine key={alt.product.id} alternative={alt} />
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}

function AlternativeLine({ alternative }: { alternative: AlternativeProduct }) {
  const t = useTranslations("Barcode");
  return (
    <li className="flex items-center justify-between text-sm">
      <span>
        {alternative.product.name}
        {alternative.product.brand ? ` · ${alternative.product.brand}` : ""}
      </span>
      <span className="font-medium">{t("score.outOf100", { score: alternative.score })}</span>
    </li>
  );
}
