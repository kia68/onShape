import { useFormatter, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";

export default function Home() {
  const t = useTranslations("Home");
  const nav = useTranslations("Nav");
  const format = useFormatter();

  const today = new Date();
  const currentWeightKg = 78.4;
  const loggedSets = 3;

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 p-8 text-center">
      <h1 className="text-3xl font-semibold">{t("title")}</h1>
      <p className="text-muted-foreground">{t("tagline")}</p>

      <nav className="flex gap-4 text-sm">
        <span>{nav("nutrition")}</span>
        <span>{nav("training")}</span>
        <span>{nav("progress")}</span>
      </nav>

      {/* Intl.NumberFormat/DateTimeFormat ueber next-intl's useFormatter (§13) */}
      <dl className="grid gap-2 text-sm">
        <div>{t("todayLabel", { date: today })}</div>
        <div>{t("weightLabel", { weight: currentWeightKg })}</div>
        <div>{t("workoutSummary", { sets: loggedSets })}</div>
        <div>
          {format.number(currentWeightKg, { style: "unit", unit: "kilogram" })}
        </div>
      </dl>

      <Button>{nav("training")}</Button>
    </main>
  );
}
