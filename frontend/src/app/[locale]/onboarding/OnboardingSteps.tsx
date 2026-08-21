"use client";

import { useTranslations } from "next-intl";
import { Field, PillCheckboxGroup, PillRadioGroup, TextInput } from "./FormControls";
import { GOAL_RATE_RANGE, type OnboardingDraft } from "./types";

type StepProps = {
  draft: OnboardingDraft;
  onChange: (patch: Partial<OnboardingDraft>) => void;
};

const SEX_VALUES = ["male", "female", "other", "unspecified"] as const;
const EXPERIENCE_VALUES = ["none", "beginner", "intermediate", "advanced"] as const;
const ACTIVITY_OPTIONS = [
  { pal: 1.25, key: "pal125" },
  { pal: 1.4, key: "pal140" },
  { pal: 1.55, key: "pal155" },
  { pal: 1.75, key: "pal175" },
] as const;
const GOAL_VALUES = ["lose", "gain_muscle", "gain_weight", "strength", "maintain", "recomp"] as const;
const EQUIPMENT_VALUES = ["gym", "dumbbells", "barbell", "bands", "pullup_bar", "kettlebell", "bodyweight"];
const DIETARY_PREF_VALUES = ["omnivore", "vegetarian", "vegan", "pescetarian", "halal", "kosher"];
const ALLERGEN_VALUES = [
  "gluten", "crustaceans", "eggs", "fish", "peanuts", "soy", "milk", "nuts",
  "celery", "mustard", "sesame", "sulphites", "lupin", "molluscs",
];
const INJURY_VALUES = ["knee", "shoulder", "low_back", "wrist", "hip"];

export function BasicsStep({ draft, onChange }: StepProps) {
  const t = useTranslations("Onboarding.basics");
  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-xl font-semibold">{t("title")}</h2>

      <Field label={t("sexLabel")}>
        <PillRadioGroup
          options={SEX_VALUES.map((v) => ({ value: v, label: t(`sex.${v}`) }))}
          value={draft.sex || "unspecified"}
          onChange={(sex) => onChange({ sex })}
        />
      </Field>

      <Field label={t("birthDateLabel")}>
        <TextInput
          type="date"
          required
          value={draft.birthDate}
          onChange={(e) => onChange({ birthDate: e.target.value })}
        />
      </Field>

      <div className="grid grid-cols-2 gap-4">
        <Field label={t("heightLabel")}>
          <TextInput
            type="number"
            required
            min={100}
            max={250}
            value={draft.heightCm}
            onChange={(e) => onChange({ heightCm: e.target.value })}
          />
        </Field>
        <Field label={t("weightLabel")}>
          <TextInput
            type="number"
            required
            min={20}
            max={400}
            value={draft.weightKg}
            onChange={(e) => onChange({ weightKg: e.target.value })}
          />
        </Field>
      </div>

      <Field label={t("bodyFatLabel")}>
        <TextInput
          type="number"
          min={3}
          max={70}
          value={draft.bodyFatPct}
          onChange={(e) => onChange({ bodyFatPct: e.target.value })}
        />
      </Field>

      <Field label={t("experienceLabel")}>
        <PillRadioGroup
          options={EXPERIENCE_VALUES.map((v) => ({ value: v, label: t(`experience.${v}`) }))}
          value={draft.experience || "none"}
          onChange={(experience) => onChange({ experience })}
        />
      </Field>

      <Field label={t("activityLabel")}>
        <PillRadioGroup
          options={ACTIVITY_OPTIONS.map((o) => ({ value: String(o.pal), label: t(`activity.${o.key}`) }))}
          value={String(draft.activityPal)}
          onChange={(v) => onChange({ activityPal: Number(v) })}
        />
      </Field>
    </div>
  );
}

export function GoalStep({ draft, onChange }: StepProps) {
  const t = useTranslations("Onboarding.goal");
  const goal = draft.goal || "maintain";
  const range = GOAL_RATE_RANGE[goal as keyof typeof GOAL_RATE_RANGE];

  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-xl font-semibold">{t("title")}</h2>

      <Field label={t("goalLabel")}>
        <PillRadioGroup
          options={GOAL_VALUES.map((v) => ({ value: v, label: t(`goal.${v}`) }))}
          value={goal}
          onChange={(newGoal) => {
            const newRange = GOAL_RATE_RANGE[newGoal as keyof typeof GOAL_RATE_RANGE];
            onChange({ goal: newGoal, goalRatePctWeek: newRange ? newRange[0] : 0 });
          }}
        />
      </Field>

      {range && (
        <Field
          label={t("rateLabel")}
          hint={t("rateHintRange", { min: range[0], max: range[1] })}
        >
          <TextInput
            type="number"
            step={0.025}
            min={range[0]}
            max={range[1]}
            value={draft.goalRatePctWeek}
            onChange={(e) => onChange({ goalRatePctWeek: Number(e.target.value) })}
          />
        </Field>
      )}
      {!range && <p className="text-xs text-muted-foreground">{t("rateHintNone")}</p>}

      <Field label={t("targetWeightLabel")}>
        <TextInput
          type="number"
          min={30}
          max={400}
          value={draft.targetWeightKg}
          onChange={(e) => onChange({ targetWeightKg: e.target.value })}
        />
      </Field>
    </div>
  );
}

export function SetupStep({ draft, onChange }: StepProps) {
  const t = useTranslations("Onboarding.setup");

  return (
    <div className="flex flex-col gap-5">
      <h2 className="text-xl font-semibold">{t("title")}</h2>

      <Field label={t("equipmentLabel")}>
        <PillCheckboxGroup
          options={EQUIPMENT_VALUES.map((v) => ({ value: v, label: t(`equipment.${v}`) }))}
          values={draft.equipment}
          onChange={(equipment) => onChange({ equipment })}
        />
      </Field>

      <div className="grid grid-cols-2 gap-4">
        <Field label={t("trainingDaysLabel")}>
          <TextInput
            type="number"
            min={1}
            max={7}
            value={draft.trainingDaysWeek}
            onChange={(e) => onChange({ trainingDaysWeek: Number(e.target.value) })}
          />
        </Field>
        <Field label={t("sessionMinutesLabel")}>
          <TextInput
            type="number"
            min={10}
            max={240}
            value={draft.sessionMinutes}
            onChange={(e) => onChange({ sessionMinutes: Number(e.target.value) })}
          />
        </Field>
      </div>

      <Field label={t("dietaryPrefsLabel")}>
        <PillCheckboxGroup
          options={DIETARY_PREF_VALUES.map((v) => ({ value: v, label: t(`dietaryPrefs.${v}`) }))}
          values={draft.dietaryPrefs}
          onChange={(dietaryPrefs) => onChange({ dietaryPrefs })}
        />
      </Field>

      <Field label={t("allergensLabel")}>
        <PillCheckboxGroup
          options={ALLERGEN_VALUES.map((v) => ({ value: v, label: t(`allergens.${v}`) }))}
          values={draft.allergens}
          onChange={(allergens) => onChange({ allergens })}
        />
      </Field>

      <Field label={t("injuriesLabel")}>
        <PillCheckboxGroup
          options={INJURY_VALUES.map((v) => ({ value: v, label: t(`injuries.${v}`) }))}
          values={draft.injuries}
          onChange={(injuries) => onChange({ injuries })}
        />
      </Field>

      <div className="flex flex-col gap-2 rounded-md border border-input p-4">
        <h3 className="text-sm font-semibold">{t("healthScreeningTitle")}</h3>
        <p className="text-xs text-muted-foreground">{t("healthScreeningHint")}</p>
        {(["heartCondition", "pregnancy", "recentInjury", "medication"] as const).map((key) => (
          <label key={key} className="flex min-h-11 items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="h-5 w-5"
              checked={draft.healthScreening[key]}
              onChange={(e) =>
                onChange({ healthScreening: { ...draft.healthScreening, [key]: e.target.checked } })
              }
            />
            {t(key)}
          </label>
        ))}
      </div>
    </div>
  );
}
