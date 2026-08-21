"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { searchFoods, type FoodSearchResult, type MealSlot } from "@/lib/nutritionApi";

const SLOTS: MealSlot[] = ["breakfast", "lunch", "dinner", "snack", "pre_workout", "post_workout"];

/** FR-22 (Quick-Add <= 3 Taps: Suche -> Ergebnis antippen -> Menge bestaetigen) und FR-23
 * (Multi-Select: mehrere Ergebnisse anhaken, in einem Rutsch loggen). */
export function QuickAddSearch({
  locale,
  defaultSlot,
  onLog,
  onLogBatch,
}: {
  locale: string;
  defaultSlot: MealSlot;
  onLog: (food: FoodSearchResult, grams: number, slot: MealSlot) => void;
  onLogBatch: (items: { food: FoodSearchResult; grams: number; slot: MealSlot }[]) => void;
}) {
  const t = useTranslations("Nutrition.search");
  const tSlot = useTranslations("Nutrition.slot");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<FoodSearchResult[]>([]);
  const [grams, setGrams] = useState<Record<string, number>>({});
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [slot, setSlot] = useState<MealSlot>(defaultSlot);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (query.trim().length < 2) return;
    debounceRef.current = setTimeout(async () => {
      const found = await searchFoods(query, locale).catch(() => []);
      setResults(found);
      setGrams((prev) => {
        const next = { ...prev };
        for (const result of found) {
          if (!(result.id in next)) {
            next[result.id] = result.lastUsedGrams ?? result.servings.find((s) => s.isDefault)?.grams ?? 100;
          }
        }
        return next;
      });
    }, 300);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, locale]);

  const toggleSelected = (id: string) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const visibleResults = query.trim().length >= 2 ? results : [];

  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2">
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={t("placeholder")}
          className="h-10 flex-1 rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
        <select
          value={slot}
          onChange={(e) => setSlot(e.target.value as MealSlot)}
          className="h-10 rounded-md border border-input bg-background px-2 text-sm"
        >
          {SLOTS.map((s) => (
            <option key={s} value={s}>
              {tSlot(s)}
            </option>
          ))}
        </select>
      </div>

      {query.trim().length >= 2 && visibleResults.length === 0 && (
        <p className="text-sm text-muted-foreground">{t("noResults")}</p>
      )}

      {selected.size > 1 && (
        <Button
          size="sm"
          onClick={() => {
            const byId = new Map(results.map((r) => [r.id, r]));
            onLogBatch(
              [...selected]
                .map((id) => byId.get(id))
                .filter((food): food is FoodSearchResult => Boolean(food))
                .map((food) => ({ food, grams: grams[food.id] ?? 100, slot })),
            );
            setSelected(new Set());
          }}
        >
          {t("logButton")} ({selected.size})
        </Button>
      )}

      <ul className="flex flex-col gap-2">
        {visibleResults.map((result) => (
          <li
            key={result.id}
            className={cn("flex items-center gap-2 rounded-md border border-input p-2", selected.has(result.id) && "border-primary")}
          >
            <input
              type="checkbox"
              className="h-5 w-5"
              checked={selected.has(result.id)}
              onChange={() => toggleSelected(result.id)}
              aria-label={result.name}
            />
            <div className="flex-1 text-sm">
              <div className="font-medium">{result.name}</div>
              <div className="text-xs text-muted-foreground">
                {Math.round(result.kcalPer100g)} kcal / 100g{result.brand ? ` · ${result.brand}` : ""}
              </div>
            </div>
            <input
              type="number"
              min={1}
              value={grams[result.id] ?? 100}
              onChange={(e) => setGrams((prev) => ({ ...prev, [result.id]: Number(e.target.value) }))}
              className="h-9 w-20 rounded-md border border-input bg-background px-2 text-sm"
            />
            <span className="text-xs text-muted-foreground">{t("gramsLabel")}</span>
            <Button size="sm" onClick={() => onLog(result, grams[result.id] ?? 100, slot)}>
              {t("logButton")}
            </Button>
          </li>
        ))}
      </ul>
    </div>
  );
}
