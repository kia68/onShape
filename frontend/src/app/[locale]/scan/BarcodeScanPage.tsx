"use client";

import { useCallback, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { toIsoDate } from "@/lib/dateUtils";
import { scanBarcode, type BarcodeScanResult } from "@/lib/barcodeApi";
import { AddProductForm } from "./AddProductForm";
import { BarcodeScanner } from "./BarcodeScanner";
import { CompareMode } from "./CompareMode";
import { ScanResult } from "./ScanResult";
import { ShoppingListMode } from "./ShoppingListMode";

type FoundResult = BarcodeScanResult & { found: true; product: NonNullable<BarcodeScanResult["product"]> };

/** FR-50 (Einkaufslisten-Modus) und FR-51 (Regal-Vergleichsmodus) sind eigene Modi neben dem
 * urspruenglichen Einzelscan -- alle drei teilen sich denselben `BarcodeScanner` (Kamera bzw.
 * manuelle Eingabe), nur die Auswertung der erkannten Barcodes unterscheidet sich. */
type ScanMode = "single" | "shoppingList" | "compare";

export function BarcodeScanPage() {
  const t = useTranslations("Barcode");
  const [mode, setMode] = useState<ScanMode>("single");
  const [result, setResult] = useState<BarcodeScanResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fromCache, setFromCache] = useState(false);
  // FR-42: Lokaler Cache fuer bereits gescannte Produkte in dieser Sitzung.
  const cache = useRef(new Map<string, BarcodeScanResult>());

  const handleDetected = useCallback(async (barcode: string) => {
    setError(null);
    setFromCache(false);
    const cached = cache.current.get(barcode);
    if (cached) {
      setResult(cached);
      setFromCache(true);
      return;
    }
    setLoading(true);
    try {
      const scanResult = await scanBarcode(barcode, toIsoDate(new Date()));
      cache.current.set(barcode, scanResult);
      setResult(scanResult);
    } catch {
      setError(t("errors.unknown_error"));
    } finally {
      setLoading(false);
    }
  }, [t]);

  return (
    <div className="mx-auto flex w-full max-w-xl flex-col gap-6 p-6">
      <h1 className="text-2xl font-semibold">{t("title")}</h1>

      <div className="flex gap-2 border-b border-input pb-2 text-sm">
        {(["single", "shoppingList", "compare"] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => { setMode(tab); setResult(null); setError(null); setFromCache(false); }}
            className={cn(
              "rounded-md px-3 py-1.5",
              mode === tab ? "bg-foreground text-background" : "text-muted-foreground hover:text-foreground",
            )}
          >
            {t(`mode.${tab}`)}
          </button>
        ))}
      </div>

      {mode === "shoppingList" && <ShoppingListMode />}
      {mode === "compare" && <CompareMode />}

      {mode === "single" && (
        <>
          {!result && <BarcodeScanner onDetected={handleDetected} />}

          {loading && <p className="text-sm text-muted-foreground">{t("scanning")}</p>}
          {error && <p className="text-sm text-destructive">{error}</p>}
          {fromCache && <p className="text-xs text-muted-foreground">{t("cachedHint")}</p>}

          {result && !result.found && (
            <div className="flex flex-col gap-4">
              <div className="rounded-md border border-input p-4">
                <h2 className="text-lg font-semibold">{t("notFound.title")}</h2>
                <p className="text-sm text-muted-foreground">{t("notFound.subtitle", { barcode: result.barcode })}</p>
              </div>
              <AddProductForm barcode={result.barcode} onCreated={() => handleDetected(result.barcode)} />
            </div>
          )}

          {result && result.found && <ScanResult result={result as FoundResult} />}

          {result && (
            <button
              type="button"
              className="self-start text-sm text-muted-foreground underline underline-offset-4"
              onClick={() => { setResult(null); setFromCache(false); }}
            >
              {t("scanAgain")}
            </button>
          )}
        </>
      )}
    </div>
  );
}
