"use client";

import { useTranslations } from "next-intl";
import type { OnboardingResultResponse } from "@/lib/api";

/** FR-11: Ergebnis-Screen mit aufklappbarer Herleitung -- KONZEPT.md §5.1 Designprinzip
 * "Jede berechnete Zahl ... erklaert sich selbst". `calculation` kommt roh vom Backend
 * (NutritionTargetCalculator.kt), Struktur ist bewusst generisch (Map<String, Any?>) statt
 * einem festen Frontend-DTO -- die Herleitung darf wachsen, ohne dass beide Seiten
 * synchron angepasst werden muessen. */
export function ResultStep({ result }: { result: OnboardingResultResponse }) {
  const t = useTranslations("Onboarding.result");

  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-xl font-semibold">{t("title")}</h2>

      {result.healthAdvisory.needsMedicalAdvice && (
        <p className="rounded-md border border-amber-500/50 bg-amber-500/10 px-3 py-2 text-sm">
          {t("medicalAdvice")}
        </p>
      )}

      <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <Stat label={t("kcal")} value={`${result.kcal} kcal`} />
        <Stat label={t("protein")} value={`${result.proteinG} g`} />
        <Stat label={t("fat")} value={`${result.fatG} g`} />
        <Stat label={t("carbs")} value={`${result.carbsG} g`} />
        <Stat label={t("fiber")} value={`${result.fiberG} g`} />
        <Stat label={t("water")} value={`${result.waterMl} ml`} />
      </dl>

      <details className="rounded-md border border-input p-4">
        <summary className="cursor-pointer text-sm font-medium">{t("explainToggle")}</summary>
        <CalculationTree value={result.calculation} className="mt-3" />
      </details>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-input p-3">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-lg font-semibold">{value}</dd>
    </div>
  );
}

/** Rendert die Herleitungs-Map rekursiv als verschachtelte Liste -- generisch genug fuer die
 * jsonb-Struktur aus nutrition_targets.calculation, egal wie sie sich weiterentwickelt. */
function CalculationTree({ value, className }: { value: unknown; className?: string }) {
  if (value === null || value === undefined) return null;

  if (Array.isArray(value)) {
    return (
      <ul className={className}>
        {value.map((item, index) => (
          <li key={index}>
            <CalculationTree value={item} />
          </li>
        ))}
      </ul>
    );
  }

  if (typeof value === "object") {
    return (
      <dl className={className}>
        {Object.entries(value as Record<string, unknown>).map(([key, val]) => (
          <div key={key} className="border-t border-input py-1.5 first:border-t-0">
            <dt className="text-xs font-medium text-muted-foreground">{key}</dt>
            <dd className="text-sm">
              {typeof val === "object" && val !== null ? <CalculationTree value={val} /> : String(val)}
            </dd>
          </div>
        ))}
      </dl>
    );
  }

  return <span className={className}>{String(value)}</span>;
}
