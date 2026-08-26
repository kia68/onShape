"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError, getToken } from "@/lib/api";
import { Link } from "@/i18n/navigation";
import { startCheckout, type CheckoutPlan } from "@/lib/billingApi";

type Period = "monthly" | "yearly";

/** BIZ-01/BIZ-02/BIZ-03 (KONZEPT.md §15.1). Oeffentliche Marketing-/Vergleichsseite -- Kauf
 * selbst braucht Auth (Checkout traegt `client_reference_id`), nicht angemeldete Besucher
 * werden stattdessen zum Onboarding geleitet. */
export function PricingPage() {
  const t = useTranslations("Pricing");
  const tErrors = useTranslations("Pricing.errors");
  const [period, setPeriod] = useState<Period>("yearly");
  const [busyPlan, setBusyPlan] = useState<CheckoutPlan | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Client-erst ermittelt (nicht direkt im Render-Body): getToken() liest localStorage, das
  // beim Server-Rendering nicht existiert -- ein synchroner Lesevorgang wuerde bei bereits
  // vorhandenem Token einen Hydration-Mismatch ausloesen (Server rendert "nicht angemeldet",
  // Client sofort "angemeldet"). Default false ist ausserdem der sichere Fallback. setTimeout
  // statt direktem Aufruf: siehe Kommentar in NutritionDayView.tsx (react-hooks/set-state-in-effect).
  const [authenticated, setAuthenticated] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setAuthenticated(Boolean(getToken())), 0);
    return () => clearTimeout(timer);
  }, []);

  const handleCheckout = async (plan: CheckoutPlan) => {
    if (!authenticated) {
      setError(t("loginRequired"));
      return;
    }
    setBusyPlan(plan);
    setError(null);
    try {
      const { checkoutUrl } = await startCheckout(plan);
      window.location.href = checkoutUrl;
    } catch (err) {
      if (err instanceof ApiError) setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      else setError(tErrors("unknown_error"));
    } finally {
      setBusyPlan(null);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-8 p-6">
      <div className="text-center">
        <h1 className="text-3xl font-semibold">{t("title")}</h1>
        <p className="mt-2 text-muted-foreground">{t("subtitle")}</p>
      </div>

      {error && <p className="text-center text-sm text-destructive">{error}</p>}

      <div className="flex justify-center gap-2">
        {(["monthly", "yearly"] as Period[]).map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => setPeriod(p)}
            className={`min-h-9 rounded-full border px-4 text-sm ${
              p === period ? "border-primary bg-primary text-primary-foreground" : "border-input bg-background"
            }`}
          >
            {t(`billingToggle.${p}`)}
          </button>
        ))}
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <TierCard title={t("tier.free.name")} price={t("tier.free.price")} cta={t("tier.free.cta")}>
          <FeatureLine label={t("features.foodLogging")} />
          <FeatureLine label={t("features.barcodeScanner")} />
          <FeatureLine label={t("features.fitScore")} value={t("features.fitScoreFree")} />
          <FeatureLine label={t("features.micronutrients")} value={t("features.micronutrientsBasis")} />
          <FeatureLine label={t("features.trainingPlans")} value={t("features.trainingPlansFree")} />
          <FeatureLine label={t("features.volumeAnalytics")} value={t("features.volumeAnalyticsBasis")} />
          <FeatureLine label={t("features.dataExport")} />
          <FeatureLine label={t("features.noAds")} />
        </TierCard>

        <TierCard
          title={t("tier.plus.name")}
          price={period === "monthly" ? t("tier.plus.priceMonthly") : t("tier.plus.priceYearly")}
          cta={t("tier.plus.cta")}
          highlighted
          busy={busyPlan === (period === "monthly" ? "plus_monthly" : "plus_yearly")}
          onSelect={() => handleCheckout(period === "monthly" ? "plus_monthly" : "plus_yearly")}
        >
          <FeatureLine label={t("features.fitScore")} value={t("features.fitScoreUnlimited")} />
          <FeatureLine label={t("features.micronutrients")} value={t("features.micronutrientsFull")} />
          <FeatureLine label={t("features.trainingPlans")} value={t("features.trainingPlansUnlimited")} />
          <FeatureLine label={t("features.volumeAnalytics")} value={t("features.volumeAnalyticsFull")} />
          <FeatureLine label={t("features.dataExport")} />
          <FeatureLine label={t("features.noAds")} />
        </TierCard>

        <TierCard
          title={t("tier.coach.name")}
          price={period === "monthly" ? t("tier.coach.priceMonthly") : t("tier.coach.priceYearly")}
          cta={t("tier.coach.cta")}
          busy={busyPlan === (period === "monthly" ? "coach_monthly" : "coach_yearly")}
          onSelect={() => handleCheckout(period === "monthly" ? "coach_monthly" : "coach_yearly")}
        >
          <FeatureLine label={t("features.fitScore")} value={t("features.fitScoreUnlimited")} />
          <FeatureLine label={t("features.micronutrients")} value={t("features.micronutrientsFull")} />
          <FeatureLine label={t("features.trainingPlans")} value={t("features.trainingPlansUnlimited")} />
          <FeatureLine label={t("features.volumeAnalytics")} value={t("features.volumeAnalyticsFull")} />
          <FeatureLine label={t("features.dataExport")} />
          <FeatureLine label={t("features.noAds")} />
        </TierCard>
      </div>

      <div className="rounded-md border border-input p-6 text-center">
        <h2 className="text-lg font-semibold">{t("lifetime.title")}</h2>
        <p className="mt-1 text-2xl font-bold">{t("lifetime.price")}</p>
        <p className="mt-2 text-sm text-muted-foreground">{t("lifetime.description")}</p>
        <Button className="mt-4" disabled={busyPlan === "lifetime"} onClick={() => handleCheckout("lifetime")}>
          {busyPlan === "lifetime" ? "…" : t("lifetime.cta")}
        </Button>
      </div>

      {!authenticated && (
        <p className="text-center text-sm text-muted-foreground">
          {t("loginRequired")} <Link href="/onboarding" className="underline underline-offset-4">{t("tier.free.cta")}</Link>
        </p>
      )}
    </div>
  );
}

function TierCard({
  title,
  price,
  cta,
  highlighted,
  busy,
  onSelect,
  children,
}: {
  title: string;
  price: string;
  cta: string;
  highlighted?: boolean;
  busy?: boolean;
  onSelect?: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className={`flex flex-col gap-4 rounded-md border p-5 ${highlighted ? "border-primary" : "border-input"}`}>
      <div>
        <h3 className="text-lg font-semibold">{title}</h3>
        <p className="text-2xl font-bold">{price}</p>
      </div>
      <ul className="flex flex-1 flex-col gap-2 text-sm text-muted-foreground">{children}</ul>
      {onSelect ? (
        <Button variant={highlighted ? "default" : "outline"} disabled={busy} onClick={onSelect}>
          {busy ? "…" : cta}
        </Button>
      ) : (
        <Button variant="outline" disabled>
          {cta}
        </Button>
      )}
    </div>
  );
}

function FeatureLine({ label, value }: { label: string; value?: string }) {
  return (
    <li className="flex items-center justify-between gap-2">
      <span>{label}</span>
      {value && <span className="font-medium text-foreground">{value}</span>}
    </li>
  );
}
