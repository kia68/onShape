"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { importHevyCsv, importStrongCsv, type ImportSummary } from "@/lib/importApi";

type Source = "hevy" | "strong";

function ImportForm({ source, onImported }: { source: Source; onImported: (summary: ImportSummary) => void }) {
  const t = useTranslations("Import");
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const summary = source === "hevy" ? await importHevyCsv(file) : await importStrongCsv(file);
      onImported(summary);
    } catch {
      setError(t("errors.unknown_error"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="rounded-md border border-input p-4">
      <h2 className="mb-1 text-sm font-semibold text-muted-foreground">{t(`${source}.title`)}</h2>
      <p className="mb-3 text-xs text-muted-foreground">{t(`${source}.hint`)}</p>
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex flex-col gap-1 text-sm">
          <span className="sr-only">{t("fileLabel")}</span>
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className="text-sm"
          />
        </label>
        <Button size="sm" disabled={!file || loading} onClick={handleSubmit}>
          {loading ? t("importing") : t(`${source}.submit`)}
        </Button>
      </div>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </section>
  );
}

function ImportResult({ summary }: { summary: ImportSummary }) {
  const t = useTranslations("Import");
  const unmatched = Object.entries(summary.unmatchedExercises);

  return (
    <section className="rounded-md border border-input p-4">
      <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("result.title")}</h2>
      <p className="text-sm">{t("result.sessions", { count: summary.sessionsImported })}</p>
      <p className="text-sm">{t("result.sets", { count: summary.setsImported })}</p>

      {unmatched.length > 0 && (
        <div className="mt-3">
          <p className="text-xs font-medium text-muted-foreground">{t("result.unmatchedTitle")}</p>
          <ul className="mt-1 flex flex-col gap-1">
            {unmatched.map(([name, count]) => (
              <li key={name} className="flex justify-between text-sm">
                <span>{name}</span>
                <span className="text-muted-foreground">{count}×</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {summary.warnings.length > 0 && (
        <div className="mt-3">
          <p className="text-xs font-medium text-muted-foreground">{t("result.warningsTitle")}</p>
          <ul className="mt-1 flex flex-col gap-1">
            {summary.warnings.map((warning, i) => (
              <li key={i} className="text-sm text-amber-600">{warning}</li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

export function ImportView() {
  const t = useTranslations("Import");
  const [result, setResult] = useState<ImportSummary | null>(null);

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-6">
      <div>
        <h1 className="text-2xl font-semibold">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("intro")}</p>
      </div>

      <ImportForm source="hevy" onImported={setResult} />
      <ImportForm source="strong" onImported={setResult} />

      {result && <ImportResult summary={result} />}

      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("unsupported.title")}</h2>
        <ul className="flex flex-col gap-2 text-sm text-muted-foreground">
          <li>{t("unsupported.myfitnesspal")}</li>
          <li>{t("unsupported.yazio")}</li>
          <li>{t("unsupported.wearables")}</li>
        </ul>
      </section>
    </div>
  );
}
