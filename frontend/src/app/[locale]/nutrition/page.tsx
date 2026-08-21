import { NutritionDayView } from "./NutritionDayView";

export default async function NutritionPage(props: PageProps<"/[locale]/nutrition">) {
  const { locale } = await props.params;
  return (
    <main className="flex flex-1 flex-col">
      <NutritionDayView locale={locale} />
    </main>
  );
}
