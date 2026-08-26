"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { addDays, toIsoDate } from "@/lib/dateUtils";
import { logFoodEntryWithOfflineFallback, logWaterWithOfflineFallback, pendingCount, registerOfflineSync } from "@/lib/offlineQueue";
import {
  copyEntries,
  deleteEntry,
  fetchDayView,
  logEntriesBatch,
  type DayView,
  type FoodEntry,
  type FoodSearchResult,
  type MealSlot,
} from "@/lib/nutritionApi";
import { fetchGuardrailStatus, type GuardrailStatusResponse } from "@/lib/legalApi";
import { QuickAddSearch } from "./QuickAddSearch";

const SLOTS: MealSlot[] = ["breakfast", "lunch", "dinner", "snack", "pre_workout", "post_workout"];

function optimisticEntry(food: FoodSearchResult, grams: number, slot: MealSlot, loggedDate: string, clientId: string): FoodEntry {
  const factor = grams / 100;
  return {
    id: clientId,
    foodId: food.id,
    recipeId: null,
    loggedDate,
    slot,
    grams,
    method: "quick_add",
    kcal: food.kcalPer100g * factor,
    proteinG: food.proteinGPer100g * factor,
    fatG: food.fatGPer100g * factor,
    carbsG: food.carbsGPer100g * factor,
    micros: {},
    clientId,
    name: food.name,
  };
}

export function NutritionDayView({ locale }: { locale: string }) {
  const t = useTranslations("Nutrition");
  const tSlot = useTranslations("Nutrition.slot");
  const tWater = useTranslations("Nutrition.water");

  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [dayView, setDayView] = useState<DayView | null>(null);
  const [pendingEntries, setPendingEntries] = useState<FoodEntry[]>([]);
  const [pending, setPending] = useState(pendingCount());
  const [error, setError] = useState<string | null>(null);
  const [guardrail, setGuardrail] = useState<GuardrailStatusResponse | null>(null);

  const load = useCallback(async () => {
    try {
      const view = await fetchDayView(date, locale);
      setDayView(view);
      setPendingEntries((prev) => prev.filter((e) => e.loggedDate !== date));
      setError(null);
    } catch {
      setError(t("errors.unknown_error"));
    }
  }, [date, locale, t]);

  useEffect(() => {
    // setTimeout statt direktem Aufruf: load() aktualisiert State erst nach einem await,
    // aber react-hooks/set-state-in-effect verfolgt den Aufruf trotzdem synchron in die
    // Funktion hinein. Der Makrotask-Umweg durchbricht diese Verfolgung (siehe auch den
    // Debounce-Timer in QuickAddSearch.tsx).
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    registerOfflineSync(() => {
      setPending(pendingCount());
      load();
    });
  }, [load]);

  useEffect(() => {
    // LEGAL-12 (KONZEPT.md §14.5): unabhaengig vom betrachteten Tag, deshalb einmalig statt an
    // [date] gekoppelt -- das Muster bezieht sich auf die letzten 7 Tage ab "heute", nicht auf
    // den gerade angezeigten Tag.
    fetchGuardrailStatus().then(setGuardrail).catch(() => setGuardrail(null));
  }, []);

  const logSingle = async (food: FoodSearchResult, grams: number, slot: MealSlot) => {
    const result = await logFoodEntryWithOfflineFallback({ foodId: food.id, loggedDate: date, slot, grams, method: "quick_add" });
    if (result.queued) {
      setPendingEntries((prev) => [...prev, optimisticEntry(food, grams, slot, date, result.clientId)]);
      setPending(pendingCount());
    } else {
      await load();
    }
  };

  const logBatch = async (items: { food: FoodSearchResult; grams: number; slot: MealSlot }[]) => {
    try {
      await logEntriesBatch(items.map((i) => ({ foodId: i.food.id, loggedDate: date, slot: i.slot, grams: i.grams, method: "quick_add" })));
      await load();
    } catch {
      for (const item of items) await logSingle(item.food, item.grams, item.slot);
    }
  };

  const handleDelete = async (id: string) => {
    if (pendingEntries.some((e) => e.id === id)) {
      setPendingEntries((prev) => prev.filter((e) => e.id !== id));
      return;
    }
    await deleteEntry(id);
    await load();
  };

  const handleCopyYesterday = async () => {
    await copyEntries(addDays(date, -1), date);
    await load();
  };

  const logWaterAmount = async (amountMl: number) => {
    const result = await logWaterWithOfflineFallback(date, amountMl);
    setPending(pendingCount());
    if (!result.queued) await load();
  };

  if (!dayView) {
    return <div className="p-6 text-sm text-muted-foreground">{error ?? "…"}</div>;
  }

  const entriesBySlot = (slot: MealSlot): FoodEntry[] => {
    const serverEntries = dayView.slots.find((s) => s.slot === slot)?.entries ?? [];
    const optimistic = pendingEntries.filter((e) => e.slot === slot);
    return [...serverEntries, ...optimistic];
  };

  const totalKcal = dayView.totalKcal + pendingEntries.reduce((sum, e) => sum + e.kcal, 0);

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <Button variant="outline" size="sm" onClick={() => setDate((d) => addDays(d, -1))}>
          {t("previousDay")}
        </Button>
        <span className="font-medium">{date}</span>
        <Button variant="outline" size="sm" onClick={() => setDate((d) => addDays(d, 1))}>
          {t("nextDay")}
        </Button>
      </div>

      {pending > 0 && (
        <p className="rounded-md border border-amber-500/50 bg-amber-500/10 px-3 py-2 text-xs">
          {t("offline.pending", { count: pending })}
        </p>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("totals")}</h2>
        {guardrail?.hideCalorieDisplay ? (
          <div className="flex flex-col gap-2 text-sm">
            <p className="font-medium">{t("wellbeing.title")}</p>
            <p className="text-muted-foreground">{t("wellbeing.message")}</p>
            {guardrail.resources.length > 0 && (
              <div>
                <p className="text-muted-foreground">{t("wellbeing.resourcesIntro")}</p>
                <ul className="list-inside list-disc text-muted-foreground">
                  {guardrail.resources.map((resource) => (
                    <li key={resource.name}>{resource.name}</li>
                  ))}
                </ul>
              </div>
            )}
            <p className="text-xs text-muted-foreground">{t("wellbeing.pauseHint")}</p>
          </div>
        ) : (
          <>
            <p className="text-2xl font-semibold">
              {dayView.targetKcal ? t("kcalOfTarget", { kcal: Math.round(totalKcal), target: dayView.targetKcal }) : t("kcalNoTarget", { kcal: Math.round(totalKcal) })}
            </p>
            <div className="mt-2 flex gap-4 text-sm text-muted-foreground">
              <span>{t("protein")}: {Math.round(dayView.totalProteinG)}g</span>
              <span>{t("fat")}: {Math.round(dayView.totalFatG)}g</span>
              <span>{t("carbs")}: {Math.round(dayView.totalCarbsG)}g</span>
            </div>
          </>
        )}
        <Button size="sm" variant="outline" className="mt-3" onClick={handleCopyYesterday}>
          {t("copyYesterday")}
        </Button>
      </div>

      <div className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{tWater("title")}</h2>
        <p className="text-lg font-semibold">
          {dayView.targetWaterMl
            ? tWater("mlOfTarget", { ml: dayView.waterMl, target: dayView.targetWaterMl })
            : tWater("mlNoTarget", { ml: dayView.waterMl })}
        </p>
        <div className="mt-2 flex gap-2">
          <Button size="sm" variant="outline" onClick={() => logWaterAmount(250)}>{tWater("add250")}</Button>
          <Button size="sm" variant="outline" onClick={() => logWaterAmount(500)}>{tWater("add500")}</Button>
        </div>
      </div>

      {SLOTS.map((slot) => {
        const entries = entriesBySlot(slot);
        return (
          <div key={slot} className="rounded-md border border-input p-4">
            <div className="mb-2 flex items-center justify-between">
              <h3 className="font-semibold">{tSlot(slot)}</h3>
              <span className="text-sm text-muted-foreground">{Math.round(entries.reduce((s, e) => s + e.kcal, 0))} kcal</span>
            </div>

            {entries.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t("emptySlot")}</p>
            ) : (
              <ul className="mb-3 flex flex-col gap-1">
                {entries.map((entry) => (
                  <li key={entry.id} className="flex items-center justify-between text-sm">
                    <span>
                      {entry.name ?? entry.foodId} · {entry.grams}g · {Math.round(entry.kcal)} kcal
                      {pendingEntries.includes(entry) && <em className="ml-1 text-xs text-amber-600"> ({t("offline.pending", { count: 1 })})</em>}
                    </span>
                    <button className="text-xs text-muted-foreground underline" onClick={() => handleDelete(entry.id)}>
                      {t("deleteEntry")}
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <QuickAddSearch locale={locale} defaultSlot={slot} onLog={logSingle} onLogBatch={logBatch} />
          </div>
        );
      })}
    </div>
  );
}
