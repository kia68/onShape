"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import { Link } from "@/i18n/navigation";
import { fetchSubscription, openBillingPortal, type SubscriptionResponse } from "@/lib/billingApi";

/** BIZ-01/BIZ-02/BIZ-03 (Epic #13): aktueller Plan + Zugang zum Stripe-Billing-Portal fuer
 * Aenderungen/Kuendigung (Stripe selbst uebernimmt Rechnungen/Zahlungsmethoden, siehe
 * StripeGateway-KDoc -- diese Seite baut kein eigenes Zahlungs-UI). */
export function BillingSettings() {
  const t = useTranslations("Settings.billing");
  const tErrors = useTranslations("Settings.billing.errors");

  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setSubscription(await fetchSubscription());
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

  const handleManage = async () => {
    setBusy(true);
    setError(null);
    try {
      const { portalUrl } = await openBillingPortal();
      window.location.href = portalUrl;
    } catch (err) {
      if (err instanceof ApiError) setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      else setError(tErrors("unknown_error"));
    } finally {
      setBusy(false);
    }
  };

  if (!subscription) {
    return <div className="p-6 text-sm text-muted-foreground">{error ?? "…"}</div>;
  }

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6 p-6">
      <h1 className="text-xl font-semibold">{t("title")}</h1>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="flex flex-col gap-2 rounded-md border border-input p-4">
        <p className="text-sm text-muted-foreground">{t("currentPlan")}</p>
        <p className="text-lg font-semibold">
          {t(`tier.${subscription.tier}`)}
          {subscription.isLifetime && <span className="ml-2 rounded-full border border-primary px-2 py-0.5 text-xs">{t("lifetimeBadge")}</span>}
        </p>
        {subscription.billingPeriod && !subscription.isLifetime && (
          <p className="text-sm text-muted-foreground">{t(`period.${subscription.billingPeriod}`)}</p>
        )}
      </div>

      {subscription.tier !== "free" && !subscription.isLifetime && (
        <Button variant="outline" disabled={busy} onClick={handleManage}>
          {busy ? "…" : t("manageButton")}
        </Button>
      )}

      <Link href="/pricing" className="text-sm text-muted-foreground underline underline-offset-4">
        {t("viewPlansLink")}
      </Link>
    </div>
  );
}
