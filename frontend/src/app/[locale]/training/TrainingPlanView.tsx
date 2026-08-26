"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import {
  fetchActiveProgram,
  fetchVolumeDashboard,
  generateProgram,
  swapExercise,
  type Program,
  type ProgramItem,
  type SwapReason,
  type VolumeDashboard,
} from "@/lib/trainingApi";

const SWAP_REASONS: SwapReason[] = ["too_hard", "equipment_occupied", "pain", "dislike", "other"];

function itemLabel(item: ProgramItem): string {
  if (item.durationMinutes != null) return `${item.durationMinutes} min`;
  return `${item.sets} × ${item.repMin}–${item.repMax}`;
}

export function TrainingPlanView({ locale }: { locale: string }) {
  const t = useTranslations("Training");
  const router = useRouter();

  const [program, setProgram] = useState<Program | null>(null);
  const [volume, setVolume] = useState<VolumeDashboard | null>(null);
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [weeksInput, setWeeksInput] = useState(6);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [swappingId, setSwappingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [planLimitExceeded, setPlanLimitExceeded] = useState(false);

  const load = useCallback(async () => {
    try {
      const active = await fetchActiveProgram();
      setProgram(active);
      setSelectedWeek((w) => Math.min(w, active.weeks) || 1);
      setError(null);
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setProgram(null);
      } else {
        setError(t("errors.unknown_error"));
      }
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    // setTimeout statt direktem Aufruf, siehe NutritionDayView.tsx (react-hooks/set-state-in-effect).
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!program) {
        setVolume(null);
        return;
      }
      fetchVolumeDashboard(selectedWeek)
        .then(setVolume)
        .catch(() => setVolume(null));
    }, 0);
    return () => clearTimeout(timer);
  }, [program, selectedWeek]);

  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    setPlanLimitExceeded(false);
    try {
      const created = await generateProgram(weeksInput);
      setProgram(created);
      setSelectedWeek(1);
    } catch (e) {
      if (e instanceof ApiError && e.code === "program_limit_exceeded") {
        setPlanLimitExceeded(true);
      } else {
        setError(t("errors.unknown_error"));
      }
    } finally {
      setGenerating(false);
    }
  };

  const handleSwap = async (item: ProgramItem, reason: SwapReason) => {
    if (!program) return;
    setSwappingId(item.id);
    setError(null);
    try {
      const result = await swapExercise(program.id, item.exerciseId, reason);
      setProgram(result.program);
    } catch (e) {
      setError(e instanceof ApiError && e.status === 422 ? t("errors.no_alternative") : t("errors.unknown_error"));
    } finally {
      setSwappingId(null);
    }
  };

  if (loading) {
    return <div className="p-6 text-sm text-muted-foreground">…</div>;
  }

  if (!program) {
    return (
      <div className="mx-auto flex w-full max-w-md flex-col gap-4 p-6">
        <h1 className="text-2xl font-semibold">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("noPlan")}</p>
        {error && <p className="text-sm text-destructive">{error}</p>}
        {planLimitExceeded && (
          <p className="text-sm text-amber-600">
            {t("errors.program_limit_exceeded")}{" "}
            <Link href="/pricing" className="underline underline-offset-4">
              {t("errors.upgradeLink")}
            </Link>
          </p>
        )}
        <label className="flex flex-col gap-1.5 text-sm">
          <span className="font-medium">{t("weeksLabel")}</span>
          <input
            type="number"
            min={2}
            max={12}
            value={weeksInput}
            onChange={(e) => setWeeksInput(Number(e.target.value))}
            className="h-10 rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </label>
        <Button onClick={handleGenerate} disabled={generating}>
          {generating ? "…" : t("generate")}
        </Button>
      </div>
    );
  }

  const days = program.days.filter((d) => d.weekNumber === selectedWeek).sort((a, b) => a.dayIndex - b.dayIndex);
  const isDeloadWeek = days.some((d) => d.isDeload);

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">{t("title")}</h1>
        <Button variant="outline" size="sm" onClick={handleGenerate} disabled={generating}>
          {generating ? "…" : t("regenerate")}
        </Button>
      </div>

      <p className="text-sm text-muted-foreground">
        {t("splitSummary", { splitType: t(`split.${program.splitType}`), daysPerWeek: program.daysPerWeek, weeks: program.weeks })}
      </p>
      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="flex flex-wrap gap-2">
        {Array.from({ length: program.weeks }, (_, i) => i + 1).map((week) => (
          <button
            key={week}
            type="button"
            onClick={() => setSelectedWeek(week)}
            className={`min-h-9 rounded-full border px-3 text-sm ${
              week === selectedWeek ? "border-primary bg-primary text-primary-foreground" : "border-input bg-background"
            }`}
          >
            {t("week", { week })}
          </button>
        ))}
      </div>
      {isDeloadWeek && (
        <p className="rounded-md border border-amber-500/50 bg-amber-500/10 px-3 py-2 text-xs">{t("deloadNotice")}</p>
      )}

      {days.map((day) => (
        <div key={day.id} className="rounded-md border border-input p-4">
          <div className="mb-2 flex items-center justify-between">
            <h3 className="font-semibold">{day.name}</h3>
            <Button size="sm" onClick={() => router.push(`/${locale}/training/log?programDayId=${day.id}`)}>
              {t("startWorkout")}
            </Button>
          </div>
          <ul className="flex flex-col gap-2">
            {day.items.map((item) => (
              <li key={item.id} className="flex items-center justify-between gap-2 text-sm">
                <span className="flex-1">
                  {item.exerciseName} · {itemLabel(item)}
                  {item.targetRir != null && ` · RIR ${item.targetRir}`}
                </span>
                <select
                  disabled={swappingId === item.id}
                  defaultValue=""
                  onChange={(e) => {
                    const reason = e.target.value as SwapReason;
                    e.target.value = "";
                    if (reason) handleSwap(item, reason);
                  }}
                  className="h-8 rounded-md border border-input bg-background px-1 text-xs"
                >
                  <option value="" disabled>
                    {t("swap.action")}
                  </option>
                  {SWAP_REASONS.map((reason) => (
                    <option key={reason} value={reason}>
                      {t(`swap.reason.${reason}`)}
                    </option>
                  ))}
                </select>
              </li>
            ))}
          </ul>
        </div>
      ))}

      {volume && (
        <div className="rounded-md border border-input p-4">
          <h2 className="mb-2 text-sm font-semibold text-muted-foreground">{t("volume.title")}</h2>
          <ul className="flex flex-col gap-2">
            {volume.entries.map((entry) => (
              <li key={entry.muscle} className="flex items-center justify-between text-sm">
                <span>{t(`volume.muscle.${entry.muscle}`)}</span>
                <span
                  className={
                    entry.status === "IN_RANGE"
                      ? "text-emerald-600"
                      : entry.status === "UNDER"
                        ? "text-amber-600"
                        : "text-destructive"
                  }
                >
                  {entry.plannedSets.toFixed(1)} / {entry.corridorMin}–{entry.corridorMax} · {t(`volume.status.${entry.status}`)}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
