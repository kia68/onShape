"use client";

import { useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";

/**
 * FR-40: `BarcodeDetector` ist eine experimentelle Web-API (Chrome/Edge/Android-Chrome, kein
 * TypeScript-lib.dom-Typ) -- deshalb die manuelle Ambient-Deklaration statt @types-Paket.
 * Spec nennt zusaetzlich einen `zxing-wasm`-Fallback fuer Browser ohne native Unterstuetzung;
 * das ist hier NICHT umgesetzt (zusaetzliche WASM-Abhaengigkeit, bewusst nicht in dieser
 * Session) -- ohne native API faellt die Komponente auf manuelle Barcode-Eingabe zurueck.
 */
declare global {
  interface BarcodeDetectorResult {
    rawValue: string;
  }
  class BarcodeDetector {
    constructor(options?: { formats: string[] });
    detect(source: CanvasImageSource): Promise<BarcodeDetectorResult[]>;
  }
  interface Window {
    BarcodeDetector?: typeof BarcodeDetector;
  }
}

const SCAN_INTERVAL_MS = 400;

export function BarcodeScanner({ onDetected }: { onDetected: (barcode: string) => void }) {
  const t = useTranslations("Barcode");
  const videoRef = useRef<HTMLVideoElement>(null);
  const [manualBarcode, setManualBarcode] = useState("");
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [supported] = useState(() => typeof window !== "undefined" && "BarcodeDetector" in window);

  useEffect(() => {
    if (!supported) return;
    let stream: MediaStream | null = null;
    let interval: ReturnType<typeof setInterval> | null = null;
    let cancelled = false;

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: "environment" } })
      .then((mediaStream) => {
        if (cancelled) {
          mediaStream.getTracks().forEach((track) => track.stop());
          return;
        }
        stream = mediaStream;
        if (videoRef.current) {
          videoRef.current.srcObject = mediaStream;
          videoRef.current.play().catch(() => undefined);
        }
        const detector = new window.BarcodeDetector!({ formats: ["ean_13", "ean_8", "upc_a", "upc_e"] });
        interval = setInterval(async () => {
          if (!videoRef.current || videoRef.current.readyState < 2) return;
          try {
            const results = await detector.detect(videoRef.current);
            if (results[0]) onDetected(results[0].rawValue);
          } catch {
            // Einzelner Frame fehlgeschlagen -- naechster Intervall-Tick versucht es erneut.
          }
        }, SCAN_INTERVAL_MS);
      })
      .catch(() => setCameraError(t("cameraPermissionDenied")));

    return () => {
      cancelled = true;
      if (interval) clearInterval(interval);
      stream?.getTracks().forEach((track) => track.stop());
    };
  }, [supported, onDetected, t]);

  const submitManual = (event: React.FormEvent) => {
    event.preventDefault();
    if (manualBarcode.trim()) onDetected(manualBarcode.trim());
  };

  return (
    <div className="flex flex-col gap-4">
      {supported && !cameraError && (
        <video ref={videoRef} className="w-full rounded-md border border-input" muted playsInline aria-label={t("scanning")} />
      )}
      {(!supported || cameraError) && (
        <p className="text-sm text-muted-foreground">{!supported ? t("cameraUnsupported") : cameraError}</p>
      )}

      <form onSubmit={submitManual} className="flex gap-2">
        <input
          type="text"
          inputMode="numeric"
          value={manualBarcode}
          onChange={(e) => setManualBarcode(e.target.value)}
          placeholder={t("manualLabel")}
          className="h-10 flex-1 rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
        <Button type="submit">{t("manualSubmit")}</Button>
      </form>
    </div>
  );
}
