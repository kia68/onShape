"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import { Link } from "@/i18n/navigation";
import {
  fetchConsents,
  fetchPauseStatus,
  pauseTracking,
  resumeTracking,
  updateConsent,
  type ConsentPurpose,
  type ConsentResponse,
  type PauseStatusResponse,
} from "@/lib/legalApi";

const PURPOSES: ConsentPurpose[] = ["core", "photo_ai", "wearable_sync", "analytics", "marketing"];

/** LEGAL-11/LEGAL-12 (Epic #12): Einwilligungen sind "jederzeit widerrufbar" (§14.1), der
 * Pausenmodus ist "ein Klick, keine Rueckgewinnungs-Kampagne" (§14.5) -- diese Seite ist der
 * einzige Ort dafuer ausserhalb des initialen Onboarding-Schritts (siehe ConsentStep.tsx). */
export function PrivacySettings() {
  const t = useTranslations("Settings.privacy");
  const tErrors = useTranslations("Settings.privacy.errors");

  const [consents, setConsents] = useState<ConsentResponse[] | null>(null);
  const [pauseStatus, setPauseStatus] = useState<PauseStatusResponse | null>(null);
  const [busyPurpose, setBusyPurpose] = useState<ConsentPurpose | null>(null);
  const [pauseBusy, setPauseBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [consentList, pause] = await Promise.all([fetchConsents(), fetchPauseStatus()]);
      setConsents(consentList);
      setPauseStatus(pause);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError) setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      else setError(tErrors("unknown_error"));
    }
  }, [tErrors]);

  useEffect(() => {
    // setTimeout statt direktem Aufruf: siehe Kommentar in NutritionDayView.tsx (gleiches
    // react-hooks/set-state-in-effect-Problem).
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const toggleConsent = async (purpose: ConsentPurpose, granted: boolean) => {
    setBusyPurpose(purpose);
    try {
      const updated = await updateConsent(purpose, granted);
      setConsents(updated);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError) setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      else setError(tErrors("unknown_error"));
    } finally {
      setBusyPurpose(null);
    }
  };

  const togglePause = async () => {
    setPauseBusy(true);
    try {
      const updated = pauseStatus?.trackingPaused ? await resumeTracking() : await pauseTracking();
      setPauseStatus(updated);
      setError(null);
    } catch (err) {
      if (err instanceof ApiError) setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      else setError(tErrors("unknown_error"));
    } finally {
      setPauseBusy(false);
    }
  };

  if (!consents || !pauseStatus) {
    return <div className="p-6 text-sm text-muted-foreground">{error ?? "…"}</div>;
  }

  const consentByPurpose = new Map(consents.map((c) => [c.purpose, c]));

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">{t("title")}</h1>
        <Link href="/settings/billing" className="text-sm text-muted-foreground underline underline-offset-4">
          {t("billingLink")}
        </Link>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <section className="flex flex-col gap-3">
        <div>
          <h2 className="font-semibold">{t("consentsTitle")}</h2>
          <p className="text-sm text-muted-foreground">{t("consentsHint")}</p>
        </div>

        {PURPOSES.map((purpose) => {
          const consent = consentByPurpose.get(purpose);
          const granted = consent?.granted ?? false;
          const isCore = purpose === "core";
          return (
            <div key={purpose} className="flex items-center justify-between gap-3 rounded-md border border-input p-3 text-sm">
              <div>
                <p className="font-medium">{t(`purpose.${purpose}`)}</p>
                <p className="text-muted-foreground">
                  {granted ? t("granted") : t("notGranted")}
                  {isCore && ` · ${t("coreLocked")}`}
                </p>
              </div>
              {!isCore && (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={busyPurpose === purpose}
                  onClick={() => toggleConsent(purpose, !granted)}
                >
                  {granted ? t("revoke") : t("grant")}
                </Button>
              )}
            </div>
          );
        })}
      </section>

      <section className="flex flex-col gap-3 rounded-md border border-input p-4">
        <div>
          <h2 className="font-semibold">{t("pauseTitle")}</h2>
          <p className="text-sm text-muted-foreground">{t("pauseDescription")}</p>
        </div>
        <p className="text-sm">
          {pauseStatus.trackingPaused && pauseStatus.trackingPausedAt
            ? t("pauseActive", { date: new Date(pauseStatus.trackingPausedAt) })
            : t("pauseInactive")}
        </p>
        <Button type="button" variant={pauseStatus.trackingPaused ? "outline" : "default"} disabled={pauseBusy} onClick={togglePause}>
          {pauseStatus.trackingPaused ? t("resumeButton") : t("pauseButton")}
        </Button>
      </section>
    </div>
  );
}
