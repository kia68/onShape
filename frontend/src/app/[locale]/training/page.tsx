import { TrainingPlanView } from "./TrainingPlanView";

export default async function TrainingPage(props: PageProps<"/[locale]/training">) {
  const { locale } = await props.params;
  return (
    <main className="flex flex-1 flex-col">
      <TrainingPlanView locale={locale} />
    </main>
  );
}
