"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { createManualProduct } from "@/lib/barcodeApi";

/** FR-49: Produkt nicht gefunden -> manuell anlegen. Etikett-Foto + OCR sind nicht umgesetzt,
 * siehe ManualProductService.kt-Kommentar (Backend) -- der Hinweis dazu ist Absicht, nicht
 * Ausrede: die App soll nicht so tun, als koennte sie etwas, das sie nicht kann. */
export function AddProductForm({ barcode, onCreated }: { barcode: string; onCreated: () => void }) {
  const t = useTranslations("Barcode.addProduct");
  const [nameDe, setNameDe] = useState("");
  const [brand, setBrand] = useState("");
  const [kcal, setKcal] = useState("");
  const [proteinG, setProteinG] = useState("");
  const [fatG, setFatG] = useState("");
  const [carbsG, setCarbsG] = useState("");
  const [sugarG, setSugarG] = useState("");
  const [fiberG, setFiberG] = useState("");
  const [saving, setSaving] = useState(false);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    try {
      await createManualProduct({
        barcode,
        nameDe,
        brand: brand || undefined,
        kcal: Number(kcal),
        proteinG: Number(proteinG),
        fatG: Number(fatG),
        carbsG: Number(carbsG),
        sugarG: sugarG ? Number(sugarG) : undefined,
        fiberG: fiberG ? Number(fiberG) : undefined,
      });
      onCreated();
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-3">
      <h3 className="text-lg font-semibold">{t("title")}</h3>
      <p className="text-xs text-muted-foreground">{t("ocrNote")}</p>

      <Field label={t("nameLabel")}><TextInput required value={nameDe} onChange={(e) => setNameDe(e.target.value)} /></Field>
      <Field label={t("brandLabel")}><TextInput value={brand} onChange={(e) => setBrand(e.target.value)} /></Field>
      <div className="grid grid-cols-2 gap-3">
        <Field label={t("kcalLabel")}><TextInput type="number" required min={0} value={kcal} onChange={(e) => setKcal(e.target.value)} /></Field>
        <Field label={t("proteinLabel")}><TextInput type="number" required min={0} value={proteinG} onChange={(e) => setProteinG(e.target.value)} /></Field>
        <Field label={t("fatLabel")}><TextInput type="number" required min={0} value={fatG} onChange={(e) => setFatG(e.target.value)} /></Field>
        <Field label={t("carbsLabel")}><TextInput type="number" required min={0} value={carbsG} onChange={(e) => setCarbsG(e.target.value)} /></Field>
        <Field label={t("sugarLabel")}><TextInput type="number" min={0} value={sugarG} onChange={(e) => setSugarG(e.target.value)} /></Field>
        <Field label={t("fiberLabel")}><TextInput type="number" min={0} value={fiberG} onChange={(e) => setFiberG(e.target.value)} /></Field>
      </div>

      <Button type="submit" disabled={saving}>{t("submit")}</Button>
    </form>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-sm">
      <span className="font-medium">{label}</span>
      {children}
    </label>
  );
}

function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className="h-10 rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
    />
  );
}
