"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { ApiError, fetchAdaptiveTdee, type AdaptiveTdeeResponse } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { addDays, toIsoDate } from "@/lib/dateUtils";
import {
  downloadCsvExport,
  downloadJsonExport,
  fetchLoggedExercises,
  fetchNutritionHistory,
  fetchOneRepMaxHistory,
  fetchVolumeHistory,
  fetchWeeklyReport,
  fetchWeightHistory,
  type LoggedExercise,
  type NutritionHistory,
  type OneRepMaxPoint,
  type WeeklyMuscleVolume,
  type WeeklyReport,
  type WeightHistory,
} from "@/lib/progressApi";

function LineChart({ raw, smoothed }: { raw: { date: string; weightKg: number }[]; smoothed: { date: string; weightKg: number }[] }) {
  if (raw.length === 0) return null;
  const width = 600;
  const height = 160;
  const padding = 20;
  const values = raw.map((p) => p.weightKg);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const toPoints = (points: { weightKg: number }[]) =>
    points
      .map((p, i) => {
        const x = padding + (i / Math.max(points.length - 1, 1)) * (width - 2 * padding);
        const y = height - padding - ((p.weightKg - min) / range) * (height - 2 * padding);
        return `${x},${y}`;
      })
      .join(" ");

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="w-full">
      <polyline points={toPoints(raw)} fill="none" stroke="currentColor" strokeOpacity={0.3} strokeWidth={1.5} />
      <polyline points={toPoints(smoothed)} fill="none" stroke="currentColor" strokeWidth={2.5} />
    </svg>
  );
}

/** Farbe fuer NEEDS_ATTENTION bewusst amber statt rot -- KONZEPT.md §14.5 Wellbeing-Guardrails:
 * "keine roten Warnfarben", wertungsfreie Sprache statt Alarmton. */
function RatingRow({ label, rating }: { label: string; rating: "GOOD" | "NEUTRAL" | "NEEDS_ATTENTION" }) {
  const color = rating === "GOOD" ? "text-emerald-600" : rating === "NEEDS_ATTENTION" ? "text-amber-600" : "text-muted-foreground";
  return <p className={`text-sm ${color}`}>{label}</p>;
}

export function ProgressView() {
  const t = useTranslations("Progress");

  const [from, setFrom] = useState(() => addDays(toIsoDate(new Date()), -30));
  const [to, setTo] = useState(() => toIsoDate(new Date()));
  const [weight, setWeight] = useState<WeightHistory | null>(null);
  const [nutrition, setNutrition] = useState<NutritionHistory | null>(null);
  const [volume, setVolume] = useState<WeeklyMuscleVolume[]>([]);
  const [exercises, setExercises] = useState<LoggedExercise[]>([]);
  const [selectedExerciseId, setSelectedExerciseId] = useState<string>("");
  const [strengthHistory, setStrengthHistory] = useState<OneRepMaxPoint[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  // FR-135 (Plus/Coach-Feature) -- eigener State/Fehlerpfad, getrennt von den anderen
  // Fortschritts-Kacheln: ein 422 hier darf die restliche Seite nicht mitreissen.
  const [weeklyReport, setWeeklyReport] = useState<WeeklyReport | null>(null);
  const [weeklyReportRequestedWeekStart, setWeeklyReportRequestedWeekStart] = useState<string | undefined>(undefined);
  const [weeklyReportRequiresUpgrade, setWeeklyReportRequiresUpgrade] = useState(false);
  const [weeklyReportError, setWeeklyReportError] = useState(false);

  // FR-134 (Plus/Coach) -- ebenfalls eigener State/Fehlerpfad.
  const [adaptiveTdee, setAdaptiveTdee] = useState<AdaptiveTdeeResponse | null>(null);
  const [adaptiveTdeeRequiresUpgrade, setAdaptiveTdeeRequiresUpgrade] = useState(false);
  const [adaptiveTdeeError, setAdaptiveTdeeError] = useState(false);

  const load = useCallback(async () => {
    try {
      const [weightResult, nutritionResult, volumeResult, exercisesResult] = await Promise.all([
        fetchWeightHistory(from, to),
        fetchNutritionHistory(from, to),
        fetchVolumeHistory(from, to),
        fetchLoggedExercises(),
      ]);
      setWeight(weightResult);
      setNutrition(nutritionResult);
      setVolume(volumeResult);
      setExercises(exercisesResult);
      setSelectedExerciseId((current) => current || exercisesResult[0]?.id || "");
      setError(null);
    } catch {
      setError(t("errors.unknown_error"));
    }
  }, [from, to, t]);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = setTimeout(async () => {
      setWeeklyReportRequiresUpgrade(false);
      setWeeklyReportError(false);
      try {
        setWeeklyReport(await fetchWeeklyReport(weeklyReportRequestedWeekStart));
      } catch (e) {
        setWeeklyReport(null);
        if (e instanceof ApiError && e.code === "weekly_report_requires_upgrade") {
          setWeeklyReportRequiresUpgrade(true);
        } else {
          setWeeklyReportError(true);
        }
      }
    }, 0);
    return () => clearTimeout(timer);
  }, [weeklyReportRequestedWeekStart]);

  useEffect(() => {
    const timer = setTimeout(async () => {
      setAdaptiveTdeeRequiresUpgrade(false);
      setAdaptiveTdeeError(false);
      try {
        setAdaptiveTdee(await fetchAdaptiveTdee());
      } catch (e) {
        setAdaptiveTdee(null);
        if (e instanceof ApiError && e.code === "adaptive_tdee_requires_upgrade") {
          setAdaptiveTdeeRequiresUpgrade(true);
        } else {
          setAdaptiveTdeeError(true);
        }
      }
    }, 0);
    return () => clearTimeout(timer);
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!selectedExerciseId) {
        setStrengthHistory([]);
        return;
      }
      fetchOneRepMaxHistory(selectedExerciseId).then(setStrengthHistory).catch(() => setStrengthHistory([]));
    }, 0);
    return () => clearTimeout(timer);
  }, [selectedExerciseId]);

  const handleExport = async (format: "json" | "csv") => {
    setExporting(true);
    try {
      await (format === "json" ? downloadJsonExport() : downloadCsvExport());
    } catch {
      setError(t("errors.unknown_error"));
    } finally {
      setExporting(false);
    }
  };

  const volumeByWeek = volume.reduce<Record<string, WeeklyMuscleVolume[]>>((acc, entry) => {
    (acc[entry.weekStart] ??= []).push(entry);
    return acc;
  }, {});

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-6">
      <h1 className="text-2xl font-semibold">{t("title")}</h1>
      {error && <p className="text-sm text-destructive">{error}</p>}

      {/* FR-135 -- eigener Block, unabhaengig vom from/to-Bereich unten (immer genau EINE Woche). */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("weeklyReport.title")}</h2>
        {weeklyReportRequiresUpgrade && (
          <p className="text-sm text-amber-600">
            {t("weeklyReport.requiresUpgrade")}{" "}
            <Link href="/pricing" className="underline underline-offset-4">{t("weeklyReport.upgradeLink")}</Link>
          </p>
        )}
        {weeklyReportError && <p className="text-sm text-muted-foreground">{t("errors.unknown_error")}</p>}
        {weeklyReport && (
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <Button
                variant="ghost" size="sm"
                onClick={() => setWeeklyReportRequestedWeekStart(addDays(weeklyReport.weekStart, -7))}
              >
                {t("weeklyReport.previousWeek")}
              </Button>
              <span className="text-xs text-muted-foreground">{weeklyReport.weekStart} – {weeklyReport.weekEnd}</span>
              <Button
                variant="ghost" size="sm"
                onClick={() => setWeeklyReportRequestedWeekStart(addDays(weeklyReport.weekStart, 7))}
              >
                {t("weeklyReport.nextWeek")}
              </Button>
            </div>

            <RatingRow
              label={t("weeklyReport.training", { completed: weeklyReport.sessionsCompleted, planned: weeklyReport.sessionsPlanned })}
              rating={weeklyReport.trainingRating}
            />
            <RatingRow
              label={t("weeklyReport.nutritionLogging", { days: weeklyReport.nutritionDaysLogged })}
              rating={weeklyReport.nutritionLoggingRating}
            />
            {weeklyReport.nutritionTargetRating && weeklyReport.avgKcal != null && weeklyReport.targetKcal != null && (
              <RatingRow
                label={t("weeklyReport.nutritionTarget", { avg: Math.round(weeklyReport.avgKcal), target: weeklyReport.targetKcal })}
                rating={weeklyReport.nutritionTargetRating}
              />
            )}
            {weeklyReport.weightChangeKg != null && (
              <p className="text-sm text-muted-foreground">
                {t("weeklyReport.weightChange", { kg: weeklyReport.weightChangeKg.toFixed(1) })}
              </p>
            )}

            <p className="mt-1 text-sm font-medium">{t(`weeklyReport.recommendation.${weeklyReport.recommendation}`)}</p>
          </div>
        )}
      </section>

      {/* FR-134 -- informativ, ersetzt NICHT das taegliche Kalorienbudget (siehe Backend-KDoc). */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("adaptiveTdee.title")}</h2>
        {adaptiveTdeeRequiresUpgrade && (
          <p className="text-sm text-amber-600">
            {t("adaptiveTdee.requiresUpgrade")}{" "}
            <Link href="/pricing" className="underline underline-offset-4">{t("adaptiveTdee.upgradeLink")}</Link>
          </p>
        )}
        {adaptiveTdeeError && <p className="text-sm text-muted-foreground">{t("errors.unknown_error")}</p>}
        {adaptiveTdee && adaptiveTdee.eligible && adaptiveTdee.adaptiveTdeeKcal != null && (
          <div className="flex flex-col gap-1">
            <p className="text-sm">{t("adaptiveTdee.eligible", { adaptive: adaptiveTdee.adaptiveTdeeKcal, formula: adaptiveTdee.formulaTdeeKcal })}</p>
            <p className="text-xs text-muted-foreground">{t("adaptiveTdee.disclaimer")}</p>
          </div>
        )}
        {adaptiveTdee && !adaptiveTdee.eligible && (
          <div className="flex flex-col gap-1">
            <p className="text-sm text-muted-foreground">{t(`adaptiveTdee.reason.${adaptiveTdee.reason}`)}</p>
            <p className="text-sm">{t("adaptiveTdee.formulaFallback", { formula: adaptiveTdee.formulaTdeeKcal })}</p>
          </div>
        )}
      </section>

      <div className="flex items-end gap-3">
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">{t("from")}</span>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="h-10 rounded-md border border-input bg-background px-2 text-sm" />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">{t("to")}</span>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="h-10 rounded-md border border-input bg-background px-2 text-sm" />
        </label>
      </div>

      {/* FR-130 */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("weight.title")}</h2>
        {weight && weight.raw.length > 0 ? (
          <>
            <LineChart raw={weight.raw} smoothed={weight.sevenDayAverage} />
            <p className="mt-1 text-xs text-muted-foreground">{t("weight.legend")}</p>
          </>
        ) : (
          <p className="text-sm text-muted-foreground">{t("weight.empty")}</p>
        )}
      </section>

      {/* FR-131 */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("nutrition.title")}</h2>
        {nutrition && (
          <>
            <p className="text-sm">
              {t("nutrition.adherence", { pct: Math.round(nutrition.adherenceRate * 100) })}
              {nutrition.targetKcal != null && ` · ${t("nutrition.target", { kcal: nutrition.targetKcal })}`}
            </p>
            <ul className="mt-3 flex flex-col gap-1">
              {nutrition.weeklyAverages.map((week) => (
                <li key={week.weekStart} className="flex justify-between text-sm">
                  <span>{t("nutrition.week", { date: week.weekStart })}</span>
                  <span>{Math.round(week.kcal)} kcal · {Math.round(week.proteinG)}g P</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      {/* FR-133 */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("volume.title")}</h2>
        {Object.keys(volumeByWeek).length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("volume.empty")}</p>
        ) : (
          Object.entries(volumeByWeek).map(([weekStart, entries]) => (
            <div key={weekStart} className="mb-3">
              <p className="text-xs font-medium text-muted-foreground">{weekStart}</p>
              <ul>
                {entries.map((entry) => {
                  const status = entry.sets < entry.corridorMin ? "under" : entry.sets > entry.corridorMax ? "over" : "in_range";
                  return (
                    <li key={entry.muscle} className="flex justify-between text-sm">
                      <span>{entry.muscle}</span>
                      <span className={status === "in_range" ? "text-emerald-600" : status === "under" ? "text-amber-600" : "text-destructive"}>
                        {entry.sets.toFixed(1)} / {entry.corridorMin}–{entry.corridorMax} · {t(`volume.status.${status}`)}
                      </span>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))
        )}
      </section>

      {/* FR-132 */}
      <section className="rounded-md border border-input p-4">
        <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("strength.title")}</h2>
        {exercises.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("strength.empty")}</p>
        ) : (
          <>
            <select
              value={selectedExerciseId}
              onChange={(e) => setSelectedExerciseId(e.target.value)}
              className="h-10 rounded-md border border-input bg-background px-2 text-sm"
            >
              {exercises.map((ex) => (
                <option key={ex.id} value={ex.id}>{ex.name}</option>
              ))}
            </select>
            <ul className="mt-3 flex flex-col gap-1">
              {strengthHistory.map((point, i) => (
                <li key={i} className="flex justify-between text-sm">
                  <span>{new Date(point.loggedAt).toLocaleDateString()}</span>
                  <span>{point.estimated1Rm.toFixed(1)} kg</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      {/* FR-137 */}
      <section className="flex gap-2">
        <Button variant="outline" size="sm" disabled={exporting} onClick={() => handleExport("json")}>{t("export.json")}</Button>
        <Button variant="outline" size="sm" disabled={exporting} onClick={() => handleExport("csv")}>{t("export.csv")}</Button>
      </section>
    </div>
  );
}
