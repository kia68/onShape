"use client";

import { useCallback, useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { fetchExerciseDetail, type ExerciseDetail } from "@/lib/movementApi";
import { ExerciseDetailCard } from "../ExerciseDetailCard";

export function ExerciseDetailPageView({ exerciseId, locale }: { exerciseId: string; locale: string }) {
  const t = useTranslations("Movement");
  const [detail, setDetail] = useState<ExerciseDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setDetail(await fetchExerciseDetail(exerciseId, locale));
      setError(null);
    } catch {
      setError(t("errors.unknown_error"));
    }
  }, [exerciseId, locale, t]);

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  if (error) return <div className="p-6 text-sm text-destructive">{error}</div>;
  if (!detail) return <div className="p-6 text-sm text-muted-foreground">…</div>;
  return <ExerciseDetailCard detail={detail} />;
}
