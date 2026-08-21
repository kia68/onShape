import { OnboardingWizard } from "./OnboardingWizard";

export default async function OnboardingPage(props: PageProps<"/[locale]/onboarding">) {
  const { locale } = await props.params;
  return (
    <main className="flex flex-1 flex-col">
      <OnboardingWizard locale={locale} />
    </main>
  );
}
