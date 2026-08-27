"use client";

import { useCallback, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { toIsoDate } from "@/lib/dateUtils";
import { scanBarcode, type BarcodeScanResult } from "@/lib/barcodeApi";
import { BarcodeScanner } from "./BarcodeScanner";

type FoundResult = BarcodeScanResult & { found: true; product: NonNullable<BarcodeScanResult["product"]> };

/** FR-51: zwei Produkte nacheinander scannen, direkter Seite-an-Seite-Vergleich. Der "Gewinner"
 * (hoeherer Fit-Score) wird nur hervorgehoben, wenn fuer BEIDE Produkte ein Score sichtbar ist --
 * bei einem BIZ-01-Monatsdeckel (`fitScoreGated`) waere ein Vergleich ohne Score fuer eine der
 * beiden Seiten irrefuehrend. */
export function CompareMode() {
  const t = useTranslations("Barcode.compare");
  const tBase = useTranslations("Barcode");
  const [results, setResults] = useState<FoundResult[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
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
      setResults((prev) => (prev.length >= 2 ? prev : [...prev, result as FoundResult]));
    } catch {
      seenRef.current.delete(barcode);
      setError(tBase("errors.unknown_error"));
    } finally {
      setLoading(false);
    }
  }, [t, tBase]);

  const reset = () => {
    seenRef.current.clear();
    setResults([]);
    setError(null);
  };

  const bothScored = results.length === 2 && results[0].score !== null && results[1].score !== null;
  const winnerIndex = bothScored ? (results[0].score! >= results[1].score! ? 0 : 1) : null;

  return (
    <div className="flex flex-col gap-4">
      {results.length < 2 && <BarcodeScanner onDetected={handleDetected} />}
      {loading && <p className="text-sm text-muted-foreground">{tBase("scanning")}</p>}
      {error && <p className="text-sm text-destructive">{error}</p>}

      {results.length > 0 && (
        <div className="grid grid-cols-2 gap-3">
          {results.map((result, index) => (
            <div
              key={result.barcode}
              className={cn(
                "flex flex-col gap-2 rounded-md border p-3 text-sm",
                winnerIndex === index ? "border-green-600 bg-green-600/10" : "border-input",
              )}
            >
              <p className="font-semibold">{result.product.name}</p>
              {result.product.brand && <p className="text-xs text-muted-foreground">{result.product.brand}</p>}
              {result.score !== null ? (
                <p className="text-lg font-bold">{tBase("score.outOf100", { score: result.score })}</p>
              ) : (
                <p className="text-xs text-amber-600 dark:text-amber-400">{tBase("fitScoreGated.title")}</p>
              )}
              <dl className="grid grid-cols-2 gap-x-2 gap-y-1 text-xs text-muted-foreground">
                <dt>{t("kcal")}</dt>
                <dd>{Math.round(result.product.kcalPer100g)}</dd>
                <dt>{t("protein")}</dt>
                <dd>{result.product.proteinGPer100g.toFixed(1)}g</dd>
                <dt>{t("fat")}</dt>
                <dd>{result.product.fatGPer100g.toFixed(1)}g</dd>
                <dt>{t("carbs")}</dt>
                <dd>{result.product.carbsGPer100g.toFixed(1)}g</dd>
              </dl>
              {winnerIndex === index && <p className="text-xs font-medium text-green-700 dark:text-green-400">{t("winner")}</p>}
            </div>
          ))}
          {results.length === 1 && (
            <div className="flex items-center justify-center rounded-md border border-dashed border-input p-3 text-center text-xs text-muted-foreground">
              {t("scanSecond")}
            </div>
          )}
        </div>
      )}

      {results.length > 0 && (
        <Button variant="outline" onClick={reset}>
          {t("newCompare")}
        </Button>
      )}
    </div>
  );
}
