"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { ExerciseDetail } from "@/lib/movementApi";

export function ExerciseDetailCard({ detail, onDismiss }: { detail: ExerciseDetail; onDismiss?: () => void }) {
  const t = useTranslations("Movement");
  const tMuscle = useTranslations("Training.volume.muscle");

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-4 p-6">
      <div>
        <h1 className="text-2xl font-bold">{detail.name}</h1>
        {detail.primaryMuscles.length > 0 && (
          <p className="mt-1 text-sm text-muted-foreground">
            {t("targetMuscles")}: {detail.primaryMuscles.map((m) => tMuscle(m)).join(", ")}
          </p>
        )}
      </div>

      <div className="flex h-32 items-center justify-center rounded-md border border-dashed border-input text-xs text-muted-foreground">
        {t("videoPlaceholder")}
      </div>

      {!detail.hasContent ? (
        <p className="rounded-md border border-input p-4 text-sm text-muted-foreground">{t("noContent")}</p>
      ) : (
        <>
          {detail.setupSteps.length > 0 && (
            <section>
              <h2 className="mb-1 text-sm font-semibold">{t("setup")}</h2>
              <ol className="list-inside list-decimal text-sm">
                {detail.setupSteps.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </section>
          )}

          {detail.executionSteps.length > 0 && (
            <section>
              <h2 className="mb-1 text-sm font-semibold">{t("execution")}</h2>
              <ol className="list-inside list-decimal text-sm">
                {detail.executionSteps.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </section>
          )}

          {detail.cues.length > 0 && (
            <section>
              <h2 className="mb-1 text-sm font-semibold">{t("cues")}</h2>
              <ul className="flex flex-wrap gap-2">
                {detail.cues.map((cue, i) => (
                  <li key={i} className="rounded-full border border-primary/50 bg-primary/10 px-3 py-1 text-sm font-medium">
                    {cue}
                  </li>
                ))}
              </ul>
            </section>
          )}

          {(detail.breathing || detail.tempo) && (
            <section className="flex gap-4 text-sm text-muted-foreground">
              {detail.breathing && <span>{t("breathing")}: {detail.breathing}</span>}
              {detail.tempo && <span>{t("tempo")}: {detail.tempo}</span>}
            </section>
          )}

          {detail.mistakes.length > 0 && (
            <section>
              <h2 className="mb-2 text-sm font-semibold">{t("commonMistakes")}</h2>
              <ul className="flex flex-col gap-3">
                {detail.mistakes.map((mistake) => (
                  <li key={mistake.id} className="rounded-md border border-input p-3 text-sm">
                    <p className="font-medium">{mistake.title}</p>
                    <p className="mt-1 text-muted-foreground">{t("why")}: {mistake.whyBad}</p>
                    <p className="mt-1 text-muted-foreground">{t("fix")}: {mistake.fix}</p>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {detail.whatIsNormal && (
            <section className="rounded-md border border-amber-500/50 bg-amber-500/10 p-3 text-sm">
              <p className="mb-1 font-semibold">{t("whatIsNormal")}</p>
              <p>{detail.whatIsNormal}</p>
            </section>
          )}
        </>
      )}

      {detail.startingWeight && (
        <section className="rounded-md border border-input p-3 text-sm">
          <p className="font-semibold">{t("startingWeightTitle")}</p>
          <p className="mt-1 text-muted-foreground">
            {detail.startingWeight.weightKg != null
              ? t("startingWeightValue", { weight: detail.startingWeight.weightKg })
              : t(`startingWeightReason.${detail.startingWeight.reasonCode}`)}
          </p>
        </section>
      )}

      {(detail.regressionOf || detail.progressionTo) && (
        <section className="flex justify-between text-sm">
          <span>{detail.regressionOf && `${t("easier")}: ${detail.regressionOf.name}`}</span>
          <span>{detail.progressionTo && `${t("harder")}: ${detail.progressionTo.name}`}</span>
        </section>
      )}

      {onDismiss && (
        <Button size="lg" onClick={onDismiss}>
          {t("dismissIntro")}
        </Button>
      )}
    </div>
  );
}
