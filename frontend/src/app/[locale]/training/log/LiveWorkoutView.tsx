"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/lib/api";
import { fetchActiveProgram, type ProgramDay } from "@/lib/trainingApi";
import {
  fetchActiveSession,
  fetchPrefill,
  finishSession,
  startSession,
  type PersonalRecord,
  type PrefillSuggestion,
  type SetTechnique,
  type WorkoutSession,
} from "@/lib/trainlogApi";
import { logWorkoutSetWithOfflineFallback, pendingCount, registerOfflineSync } from "@/lib/offlineQueue";
import { fetchExerciseDetail, type ExerciseDetail } from "@/lib/movementApi";
import { ExerciseDetailCard } from "../exercises/ExerciseDetailCard";

interface PlannedSlot {
  exerciseId: string;
  exerciseName: string;
  slotIndexForExercise: number;
  repMin: number | null;
  repMax: number | null;
  targetRir: number | null;
  restSeconds: number;
  durationMinutes: number | null;
}

function buildPlannedSlots(day: ProgramDay): PlannedSlot[] {
  const slots: PlannedSlot[] = [];
  for (const item of day.items) {
    for (let i = 0; i < item.sets; i++) {
      slots.push({
        exerciseId: item.exerciseId,
        exerciseName: item.exerciseName,
        slotIndexForExercise: i,
        repMin: item.repMin,
        repMax: item.repMax,
        targetRir: item.targetRir,
        restSeconds: item.restSeconds,
        durationMinutes: item.durationMinutes,
      });
    }
  }
  return slots;
}

function requestWakeLock(): Promise<{ release: () => Promise<void> } | null> {
  if (typeof navigator === "undefined" || !("wakeLock" in navigator)) return Promise.resolve(null);
  return navigator.wakeLock.request("screen").catch(() => null);
}

function playRestEndSound() {
  try {
    const AudioContextClass = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new AudioContextClass();
    const oscillator = ctx.createOscillator();
    oscillator.frequency.value = 880;
    oscillator.connect(ctx.destination);
    oscillator.start();
    oscillator.stop(ctx.currentTime + 0.3);
  } catch {
    // Audio nicht verfuegbar (z.B. Autoplay-Policy) -- Vibration bleibt als Fallback.
  }
  if (typeof navigator !== "undefined" && "vibrate" in navigator) navigator.vibrate(300);
}

export function LiveWorkoutView({ locale }: { locale: string }) {
  const t = useTranslations("TrainingLog");
  const router = useRouter();
  const searchParams = useSearchParams();
  const programDayIdParam = searchParams.get("programDayId");

  const [session, setSession] = useState<WorkoutSession | null>(null);
  const [day, setDay] = useState<ProgramDay | null>(null);
  const [slots, setSlots] = useState<PlannedSlot[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [prefill, setPrefill] = useState<PrefillSuggestion | null>(null);
  const [weightInput, setWeightInput] = useState("");
  const [repsInput, setRepsInput] = useState("");
  const [rirInput, setRirInput] = useState("");
  const [restRemaining, setRestRemaining] = useState<number | null>(null);
  const [celebration, setCelebration] = useState<PersonalRecord[] | null>(null);
  // FR-95: waehrend eines Drop-/Cluster-Satzes bleibt der Slot aktiv (kein Fortschritt in
  // currentIndex) -- technique != null zeigt an, dass der naechste Log ein Teilsatz derselben
  // Gruppe ist; subSetIndex zaehlt ab 0 hoch (siehe V20-Migrationskommentar).
  const [technique, setTechnique] = useState<SetTechnique | null>(null);
  const [subSetIndex, setSubSetIndex] = useState(0);
  const [pending, setPending] = useState(pendingCount());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [introExercise, setIntroExercise] = useState<ExerciseDetail | null>(null);
  const dismissedIntroIds = useRef<Set<string>>(new Set());
  const wakeLockRef = useRef<{ release: () => Promise<void> } | null>(null);

  const load = useCallback(async () => {
    try {
      let active: WorkoutSession;
      try {
        active = await fetchActiveSession();
      } catch (e) {
        // "Keine aktive Session" ist der Normalfall bei einem neuen Workout -- der Server
        // antwortet dafuer mit einem leeren 404-Body, den ein 401 nicht von unterscheidet, wenn
        // die einzige Absicherung `e.status === 404` waere. Ein echter Auth-Fehler faellt hier
        // trotzdem sauber durch: der nachfolgende startSession()-Aufruf scheitert dann ebenso.
        if (e instanceof ApiError && (e.status === 404 || e.status === 401) && programDayIdParam) {
          active = await startSession(programDayIdParam);
        } else {
          throw e;
        }
      }
      setSession(active);

      const activeProgram = await fetchActiveProgram();
      const targetDayId = active.programDayId ?? programDayIdParam;
      const foundDay = activeProgram.days.find((d) => d.id === targetDayId) ?? null;
      setDay(foundDay);
      setSlots(foundDay ? buildPlannedSlots(foundDay) : []);
      setError(null);
    } catch {
      setError(t("errors.unknown_error"));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t]);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  useEffect(() => {
    const timer = setTimeout(() => {
      requestWakeLock().then((lock) => {
        wakeLockRef.current = lock;
      });
    }, 0);
    return () => {
      clearTimeout(timer);
      wakeLockRef.current?.release();
    };
  }, []);

  useEffect(() => {
    registerOfflineSync(() => setPending(pendingCount()));
  }, []);

  const currentSlot = slots[currentIndex] ?? null;

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!currentSlot) {
        setPrefill(null);
        return;
      }
      fetchPrefill(currentSlot.exerciseId, currentSlot.repMax ?? undefined, currentSlot.targetRir ?? undefined)
        .then((result) => {
          setPrefill(result);
          setWeightInput(result.suggestedWeightKg != null ? String(result.suggestedWeightKg) : "");
          setRepsInput(result.suggestedReps != null ? String(result.suggestedReps) : "");
          setRirInput("");
        })
        .catch(() => setPrefill(null));
    }, 0);
    return () => clearTimeout(timer);
  }, [currentSlot]);

  // FR-111: vor der ersten Ausfuehrung einer Uebung im Anfaengermodus wird die Anleitung
  // automatisch eingeblendet -- nur beim ERSTEN Slot dieser Uebung in diesem Workout geprueft
  // (die weiteren Saetze derselben Uebung sollen nicht jedes Mal erneut unterbrechen).
  useEffect(() => {
    if (!currentSlot) return;
    const isFirstSlotForExercise = slots.findIndex((s) => s.exerciseId === currentSlot.exerciseId) === currentIndex;
    if (!isFirstSlotForExercise || dismissedIntroIds.current.has(currentSlot.exerciseId)) return;
    const timer = setTimeout(() => {
      fetchExerciseDetail(currentSlot.exerciseId, locale)
        .then((detail) => {
          if (detail.showBeginnerIntro) setIntroExercise(detail);
        })
        .catch(() => undefined);
    }, 0);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentSlot, currentIndex]);

  const dismissIntro = () => {
    if (introExercise) dismissedIntroIds.current.add(introExercise.id);
    setIntroExercise(null);
  };

  useEffect(() => {
    if (restRemaining == null) return;
    const timer = setTimeout(() => {
      if (restRemaining <= 0) {
        playRestEndSound();
        setRestRemaining(null);
        return;
      }
      setRestRemaining((r) => (r != null ? r - 1 : null));
    }, restRemaining <= 0 ? 0 : 1000);
    return () => clearTimeout(timer);
  }, [restRemaining]);

  const logCurrentInputs = async (opts: { setTechnique?: SetTechnique; subSetIndex?: number }) => {
    if (!session || !currentSlot) return;
    setCelebration(null);
    const result = await logWorkoutSetWithOfflineFallback(session.id, {
      exerciseId: currentSlot.exerciseId,
      setIndex: currentSlot.slotIndexForExercise,
      weightKg: weightInput ? Number(weightInput) : undefined,
      reps: repsInput ? Number(repsInput) : undefined,
      rir: rirInput ? Number(rirInput) : undefined,
      isWarmup: false,
      completed: true,
      setTechnique: opts.setTechnique,
      subSetIndex: opts.subSetIndex,
    });
    setPending(pendingCount());
    if (result.result?.personalRecords.length) setCelebration(result.result.personalRecords);
    setWeightInput("");
    setRepsInput("");
  };

  const advanceToNextSlot = () => {
    if (!currentSlot) return;
    setTechnique(null);
    setSubSetIndex(0);
    if (currentSlot.restSeconds > 0 && currentIndex < slots.length - 1) setRestRemaining(currentSlot.restSeconds);
    setCurrentIndex((i) => i + 1);
  };

  const handleLogSet = async () => {
    await logCurrentInputs({});
    advanceToNextSlot();
  };

  /** FR-95: startet eine Drop-/Cluster-Satzgruppe -- der aktuell eingegebene Satz wird als
   * Hauptsatz (subSetIndex 0) geloggt, der Slot bleibt danach aktiv fuer weitere Teilsaetze. */
  const handleStartTechnique = async (tech: SetTechnique) => {
    await logCurrentInputs({ setTechnique: tech, subSetIndex: 0 });
    setTechnique(tech);
    setSubSetIndex(1);
  };

  const handleContinueTechnique = async () => {
    if (!technique) return;
    await logCurrentInputs({ setTechnique: technique, subSetIndex });
    setSubSetIndex((i) => i + 1);
  };

  const handleFinish = async () => {
    if (!session) return;
    await finishSession(session.id);
    wakeLockRef.current?.release();
    router.push(`/${locale}/training`);
  };

  if (loading) return <div className="p-6 text-sm text-muted-foreground">…</div>;
  if (error || !session) {
    return (
      <div className="mx-auto max-w-md p-6">
        <p className="text-sm text-destructive">{error ?? t("errors.noSession")}</p>
        <Button className="mt-4" onClick={() => router.push(`/${locale}/training`)}>{t("backToPlan")}</Button>
      </div>
    );
  }

  const allSetsDone = slots.length > 0 && currentIndex >= slots.length;

  if (introExercise) return <ExerciseDetailCard detail={introExercise} onDismiss={dismissIntro} />;

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-6 p-6">
      {pending > 0 && (
        <p className="rounded-md border border-amber-500/50 bg-amber-500/10 px-3 py-2 text-xs">{t("offlinePending", { count: pending })}</p>
      )}

      {celebration && celebration.length > 0 && (
        <div className="rounded-md border border-emerald-500/50 bg-emerald-500/10 px-3 py-2 text-sm">
          🎉 {t("newRecord")}: {celebration.map((r) => t(`recordType.${r.type}`)).join(", ")}
        </div>
      )}

      {restRemaining != null && (
        <div className="rounded-md border border-primary/50 bg-primary/10 px-3 py-4 text-center">
          <p className="text-xs text-muted-foreground">{t("restTimer")}</p>
          <p className="text-3xl font-semibold">{restRemaining}s</p>
        </div>
      )}

      {allSetsDone || !currentSlot ? (
        <div className="flex flex-col gap-4 text-center">
          <h1 className="text-2xl font-semibold">{t("workoutDone")}</h1>
          <p className="text-sm text-muted-foreground">{day?.name}</p>
          <Button onClick={handleFinish}>{t("finishWorkout")}</Button>
        </div>
      ) : (
        <>
          <div className="text-center">
            <p className="text-xs text-muted-foreground">
              {t("setProgress", { current: currentIndex + 1, total: slots.length })}
            </p>
            <h1 className="text-3xl font-bold">{currentSlot.exerciseName}</h1>
            <Link href={`/${locale}/training/exercises/${currentSlot.exerciseId}`} className="text-xs text-primary underline">
              {t("viewInstructions")}
            </Link>
            <p className="text-sm text-muted-foreground">
              {currentSlot.durationMinutes != null
                ? t("targetDuration", { minutes: currentSlot.durationMinutes })
                : t("targetReps", { min: currentSlot.repMin ?? 0, max: currentSlot.repMax ?? 0 })}
              {currentSlot.targetRir != null && ` · RIR ${currentSlot.targetRir}`}
            </p>
          </div>

          {prefill?.lastWeightKg != null && (
            <p className="text-center text-xs text-muted-foreground">
              {t("lastTime", { weight: prefill.lastWeightKg, reps: prefill.lastReps ?? "–" })}
            </p>
          )}

          {/* FR-94: nur vor dem ERSTEN Satz dieser Uebung in diesem Workout, nicht vor jedem
              einzelnen Arbeitssatz erneut. Rein informativ, kein eigenes Logging pro Aufwaermsatz
              (gleiche Zurueckhaltung wie beim FR-79-Deload-Hinweis). */}
          {prefill && prefill.warmupSets.length > 0 && slots.findIndex((s) => s.exerciseId === currentSlot.exerciseId) === currentIndex && (
            <div className="rounded-md border border-input p-3 text-center text-sm">
              <p className="mb-1 text-xs font-medium text-muted-foreground">{t("warmup.title")}</p>
              <p>{prefill.warmupSets.map((w) => `${w.weightKg} kg × ${w.reps}`).join("  →  ")}</p>
            </div>
          )}

          <div className="flex gap-3">
            <label className="flex flex-1 flex-col gap-1 text-sm">
              <span className="font-medium">{t("weightLabel")}</span>
              <input
                type="number"
                inputMode="decimal"
                value={weightInput}
                onChange={(e) => setWeightInput(e.target.value)}
                className="h-12 rounded-md border border-input bg-background px-3 text-center text-lg outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </label>
            <label className="flex flex-1 flex-col gap-1 text-sm">
              <span className="font-medium">{t("repsLabel")}</span>
              <input
                type="number"
                inputMode="numeric"
                value={repsInput}
                onChange={(e) => setRepsInput(e.target.value)}
                className="h-12 rounded-md border border-input bg-background px-3 text-center text-lg outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </label>
            <label className="flex w-20 flex-col gap-1 text-sm">
              <span className="font-medium">RIR</span>
              <input
                type="number"
                inputMode="numeric"
                value={rirInput}
                onChange={(e) => setRirInput(e.target.value)}
                className="h-12 rounded-md border border-input bg-background px-2 text-center text-lg outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </label>
          </div>

          {technique && (
            <p className="text-center text-xs text-muted-foreground">
              {t(technique === "dropset" ? "technique.dropsetActive" : "technique.clusterActive", { n: subSetIndex })}
            </p>
          )}

          {technique ? (
            <div className="flex gap-3">
              <Button size="lg" className="flex-1" onClick={handleContinueTechnique}>{t("technique.continue")}</Button>
              <Button variant="outline" size="lg" onClick={advanceToNextSlot}>{t("technique.finish")}</Button>
            </div>
          ) : (
            <>
              <Button size="lg" onClick={handleLogSet}>{t("logSet")}</Button>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" className="flex-1" onClick={() => handleStartTechnique("dropset")}>
                  {t("technique.startDropset")}
                </Button>
                <Button variant="outline" size="sm" className="flex-1" onClick={() => handleStartTechnique("cluster")}>
                  {t("technique.startCluster")}
                </Button>
              </div>
            </>
          )}
          <Button variant="outline" size="sm" onClick={handleFinish}>{t("finishWorkout")}</Button>
        </>
      )}
    </div>
  );
}
