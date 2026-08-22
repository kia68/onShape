import { Suspense } from "react";
import { LiveWorkoutView } from "./LiveWorkoutView";

export default async function TrainingLogPage(props: PageProps<"/[locale]/training/log">) {
  const { locale } = await props.params;
  return (
    <main className="flex flex-1 flex-col">
      <Suspense fallback={<div className="p-6 text-sm text-muted-foreground">…</div>}>
        <LiveWorkoutView locale={locale} />
      </Suspense>
    </main>
  );
}
