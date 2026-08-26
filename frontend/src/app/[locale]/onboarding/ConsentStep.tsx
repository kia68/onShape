"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { ApiError } from "@/lib/api";
import { submitConsents } from "@/lib/legalApi";
import { ErrorNotice } from "./FormControls";

/** LEGAL-11 (KONZEPT.md §14.1): eigener Schritt, nicht in den AGB versteckt, nichts
 * vorangekreuzt ausser CORE (ohne CORE gibt es keine Rechtsgrundlage fuer die App selbst --
 * siehe ConsentService-KDoc, Backend). Ablehnung der anderen vier Zwecke blockiert den weiteren
 * Onboarding-Flow nicht. */
export function ConsentStep({ onSubmitted }: { onSubmitted: () => void }) {
  const t = useTranslations("Onboarding.consent");
  const tErrors = useTranslations("Onboarding.errors");
  const [photoAi, setPhotoAi] = useState(false);
  const [wearableSync, setWearableSync] = useState(false);
  const [analytics, setAnalytics] = useState(false);
  const [marketing, setMarketing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await submitConsents({ core: true, photoAi, wearableSync, analytics, marketing });
      onSubmitted();
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

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold">{t("title")}</h2>
        <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
      </div>

      <ErrorNotice message={error} />

      <ConsentRow label={t("core.label")} description={t("core.description")} checked disabled />
      <ConsentRow label={t("photoAi.label")} description={t("photoAi.description")} checked={photoAi} onChange={setPhotoAi} />
      <ConsentRow label={t("wearableSync.label")} description={t("wearableSync.description")} checked={wearableSync} onChange={setWearableSync} />
      <ConsentRow label={t("analytics.label")} description={t("analytics.description")} checked={analytics} onChange={setAnalytics} />
      <ConsentRow label={t("marketing.label")} description={t("marketing.description")} checked={marketing} onChange={setMarketing} />

      <p className="text-xs text-muted-foreground">{t("revocableHint")}</p>

      <Button type="button" disabled={submitting} onClick={submit}>
        {t("submit")}
      </Button>
    </div>
  );
}

function ConsentRow({
  label,
  description,
  checked,
  disabled,
  onChange,
}: {
  label: string;
  description: string;
  checked: boolean;
  disabled?: boolean;
  onChange?: (checked: boolean) => void;
}) {
  return (
    <label className={cn("flex items-start gap-3 rounded-md border border-input p-3 text-sm", disabled && "opacity-70")}>
      <input
        type="checkbox"
        className="mt-0.5 h-5 w-5 shrink-0"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange?.(e.target.checked)}
      />
      <span>
        <span className="block font-medium">{label}</span>
        <span className="block text-muted-foreground">{description}</span>
      </span>
    </label>
  );
}
