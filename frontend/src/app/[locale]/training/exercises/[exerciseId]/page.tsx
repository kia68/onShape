import { ExerciseDetailPageView } from "./ExerciseDetailPageView";

export default async function ExerciseDetailPage(props: PageProps<"/[locale]/training/exercises/[exerciseId]">) {
  const { locale, exerciseId } = await props.params;
  return (
    <main className="flex flex-1 flex-col">
      <ExerciseDetailPageView exerciseId={exerciseId} locale={locale} />
    </main>
  );
}
