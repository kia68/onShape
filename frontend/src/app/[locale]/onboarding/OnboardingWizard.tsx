"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError, getToken, submitOnboarding, type OnboardingResultResponse } from "@/lib/api";
import { AccountStep } from "./AccountStep";
import { ConsentStep } from "./ConsentStep";
import { ErrorNotice } from "./FormControls";
import { BasicsStep, GoalStep, SetupStep } from "./OnboardingSteps";
import { ResultStep } from "./ResultStep";
import { DEFAULT_DRAFT, draftToRequest, fillRequiredDefaults, type OnboardingDraft } from "./types";

// FR-01..FR-11 als ein Flow. "account" zaehlt in der Fortschrittsanzeige mit (FR-10:
// Onboarding <= 90s beginnt bei der Registrierung, nicht erst beim Profil). "consent"
// (LEGAL-11, Epic #12) ist ein eigener Schritt direkt nach der Registrierung, nicht in den
// AGB versteckt (KONZEPT.md §14.1) -- vor jeder weiteren Profilangabe.
const STEPS = ["account", "consent", "basics", "goal", "setup"] as const;

export function OnboardingWizard({ locale }: { locale: string }) {
  const t = useTranslations("Onboarding");
  const tErrors = useTranslations("Onboarding.errors");
  const [stepIndex, setStepIndex] = useState(0);
  const [authenticated, setAuthenticated] = useState(() => Boolean(getToken()));
  const [draft, setDraft] = useState<OnboardingDraft>(DEFAULT_DRAFT);
  const [result, setResult] = useState<OnboardingResultResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const step = STEPS[stepIndex];
  const patchDraft = (patch: Partial<OnboardingDraft>) => setDraft((d) => ({ ...d, ...patch }));

  const submit = async (draftToSubmit: OnboardingDraft) => {
    setSubmitting(true);
    setError(null);
    try {
      const response = await submitOnboarding(draftToRequest(draftToSubmit));
      setResult(response);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      } else {
        setError(tErrors("unknown_error"));
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (result) {
    return (
      <div className="mx-auto flex w-full max-w-xl flex-col gap-6 p-6">
        <ResultStep result={result} />
      </div>
    );
  }

  if (!authenticated || step === "account") {
    return (
      <div className="mx-auto flex w-full max-w-md flex-col gap-6 p-6">
        <AccountStep locale={locale} onAuthenticated={() => { setAuthenticated(true); setStepIndex(1); }} />
      </div>
    );
  }

  if (step === "consent") {
    return (
      <div className="mx-auto flex w-full max-w-md flex-col gap-6 p-6">
        <ConsentStep onSubmitted={() => setStepIndex(2)} />
      </div>
    );
  }

  const isLastStep = stepIndex === STEPS.length - 1;

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6 p-6">
      <ProgressBar step={stepIndex + 1} total={STEPS.length} label={t("progressLabel", { step: stepIndex + 1, total: STEPS.length })} />

      <ErrorNotice message={error} />

      {step === "basics" && <BasicsStep draft={draft} onChange={patchDraft} />}
      {step === "goal" && <GoalStep draft={draft} onChange={patchDraft} />}
      {step === "setup" && <SetupStep draft={draft} onChange={patchDraft} />}

      <div className="flex items-center justify-between gap-3">
        <Button type="button" variant="outline" disabled={stepIndex <= 2 || submitting} onClick={() => setStepIndex((i) => i - 1)}>
          {t("back")}
        </Button>

        <div className="flex gap-3">
          <button
            type="button"
            className="text-sm text-muted-foreground underline underline-offset-4"
            disabled={submitting}
            onClick={() => submit(fillRequiredDefaults(draft))}
          >
            {t("skipWithDefaults")}
          </button>

          {isLastStep ? (
            <Button type="button" disabled={submitting} onClick={() => submit(draft)}>
              {t("next")}
            </Button>
          ) : (
            <Button type="button" disabled={submitting} onClick={() => setStepIndex((i) => i + 1)}>
              {t("next")}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function ProgressBar({ step, total, label }: { step: number; total: number; label: string }) {
  const pct = Math.round((step / total) * 100);
  return (
    <div className="flex flex-col gap-1.5">
      <div className="h-2 w-full overflow-hidden rounded-full bg-secondary" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-muted-foreground">{label}</span>
    </div>
  );
}
