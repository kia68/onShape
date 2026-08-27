"use client";

import { useCallback, useRef, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { toIsoDate } from "@/lib/dateUtils";
import { scanBarcode, type BarcodeScanResult } from "@/lib/barcodeApi";
import { fetchDayView } from "@/lib/nutritionApi";
import { BarcodeScanner } from "./BarcodeScanner";

type FoundResult = BarcodeScanResult & { found: true; product: NonNullable<BarcodeScanResult["product"]> };

interface ShoppingItem {
  barcode: string;
  result: FoundResult;
}

interface Summary {
  totalProteinG: number;
  targetProteinG: number | null;
}

/** FR-50: mehrere Produkte hintereinander scannen, am Ende eine Gesamtuebersicht ("deckt X Tage
 * Protein"). `seenRef` verhindert doppelte Eintraege, wenn derselbe Barcode mehrfach hintereinander
 * erkannt wird, bevor der erste Scan-Request zurueckkommt (die Kamera erkennt denselben Barcode
 * alle 400ms neu, solange das Produkt im Bild bleibt). */
export function ShoppingListMode() {
  const t = useTranslations("Barcode.shoppingList");
  const tBase = useTranslations("Barcode");
  const locale = useLocale();
  const [items, setItems] = useState<ShoppingItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null);
  const seenRef = useRef<Set<string>>(new Set());

  const handleDetected = useCallback(async (barcode: string) => {
    if (seenRef.current.has(barcode)) return;
    seenRef.current.add(barcode);
    setError(null);
    setLoading(true);
    try {
      const result = await scanBarcode(barcode, toIsoDate(new Date()));
      if (!result.found || !result.product) {
        seenRef.current.delete(barcode);
        setError(t("notFound", { barcode }));
        return;
      }
      setItems((prev) => [...prev, { barcode, result: result as FoundResult }]);
    } catch {
      seenRef.current.delete(barcode);
      setError(tBase("errors.unknown_error"));
    } finally {
      setLoading(false);
    }
  }, [t, tBase]);

  const removeItem = (barcode: string) => {
    seenRef.current.delete(barcode);
    setItems((prev) => prev.filter((i) => i.barcode !== barcode));
  };

  const finish = async () => {
    const totalProteinG = items.reduce((sum, item) => {
      const p = item.result.product;
      return sum + p.proteinGPer100g * (p.defaultServingGrams / 100);
    }, 0);
    let targetProteinG: number | null = null;
    try {
      const day = await fetchDayView(toIsoDate(new Date()), locale);
      targetProteinG = day.targetProteinG;
    } catch {
      // Tagesziel nicht ladbar (z.B. Onboarding nicht abgeschlossen) -- Uebersicht zeigt dann nur die Rohsumme.
    }
    setSummary({ totalProteinG, targetProteinG });
  };

  const reset = () => {
    seenRef.current.clear();
    setItems([]);
    setSummary(null);
    setError(null);
  };

  if (summary) {
    return (
      <div className="flex flex-col gap-4">
        <div className="rounded-md border border-input p-4">
          <h2 className="text-lg font-semibold">{t("summary.title")}</h2>
          <p className="mt-2 text-sm text-muted-foreground">{t("summary.itemCount", { count: items.length })}</p>
          <p className="text-sm">{t("summary.totalProtein", { g: Math.round(summary.totalProteinG) })}</p>
          {summary.targetProteinG != null && summary.targetProteinG > 0 ? (
            <p className="mt-2 text-base font-medium">
              {t("summary.coversDays", { days: (summary.totalProteinG / summary.targetProteinG).toFixed(1) })}
            </p>
          ) : (
            <p className="mt-2 text-xs text-muted-foreground">{t("summary.noTarget")}</p>
          )}
        </div>
        <Button onClick={reset}>{t("newList")}</Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <BarcodeScanner onDetected={handleDetected} />
      {loading && <p className="text-sm text-muted-foreground">{tBase("scanning")}</p>}
      {error && <p className="text-sm text-destructive">{error}</p>}

      {items.length > 0 && (
        <ul className="flex flex-col gap-2 rounded-md border border-input p-3">
          {items.map((item) => (
            <li key={item.barcode} className="flex items-center justify-between text-sm">
              <span>
                {item.result.product.name}
                {item.result.product.brand ? ` · ${item.result.product.brand}` : ""}
              </span>
              <button
                type="button"
                className="text-xs text-muted-foreground underline underline-offset-4"
                onClick={() => removeItem(item.barcode)}
              >
                {t("remove")}
              </button>
            </li>
          ))}
        </ul>
      )}

      {items.length > 0 && <Button onClick={finish}>{t("finish")}</Button>}
    </div>
  );
}
