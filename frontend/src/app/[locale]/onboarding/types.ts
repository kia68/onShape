import type { OnboardingRequest } from "@/lib/api";

/** Formular-Entwurf: wie OnboardingRequest, aber mit Leerstrings/undefined waehrend der Eingabe erlaubt. */
export interface OnboardingDraft {
  sex: OnboardingRequest["sex"] | "";
  birthDate: string;
  heightCm: string;
  weightKg: string;
  bodyFatPct: string;
  experience: OnboardingRequest["experience"] | "";
  activityPal: number;
  goal: OnboardingRequest["goal"] | "";
  goalRatePctWeek: number;
  targetWeightKg: string;
  dietaryPrefs: string[];
  allergens: string[];
  injuries: string[];
  equipment: string[];
  trainingDaysWeek: number;
  sessionMinutes: number;
  healthScreening: OnboardingRequest["healthScreening"];
}

/** FR-10: "jederzeit abbrechbar mit Defaults" -- entspricht den DB-Spalten-Defaults aus V1__extensions_users_profile.sql. */
export const DEFAULT_DRAFT: OnboardingDraft = {
  sex: "unspecified",
  birthDate: "",
  heightCm: "",
  weightKg: "",
  bodyFatPct: "",
  experience: "none",
  activityPal: 1.4,
  goal: "maintain",
  goalRatePctWeek: 0,
  targetWeightKg: "",
  dietaryPrefs: [],
  allergens: [],
  injuries: [],
  equipment: ["bodyweight"],
  trainingDaysWeek: 3,
  sessionMinutes: 60,
  healthScreening: { heartCondition: false, pregnancy: false, recentInjury: false, medication: false },
};

/** KONZEPT.md §5.1 FR-04: medizinische Grenzen pro Ziel, muss mit GoalRateValidator.kt uebereinstimmen. */
export const GOAL_RATE_RANGE: Record<OnboardingRequest["goal"], [number, number] | null> = {
  lose: [0.25, 1.0],
  gain_muscle: [0.125, 0.5],
  gain_weight: [0.125, 0.5],
  strength: null,
  maintain: null,
  recomp: null,
};

export function draftToRequest(draft: OnboardingDraft): OnboardingRequest {
  return {
    sex: draft.sex as OnboardingRequest["sex"],
    birthDate: draft.birthDate,
    heightCm: Number(draft.heightCm),
    weightKg: Number(draft.weightKg),
    bodyFatPct: draft.bodyFatPct ? Number(draft.bodyFatPct) : undefined,
    experience: draft.experience as OnboardingRequest["experience"],
    activityPal: draft.activityPal,
    goal: draft.goal as OnboardingRequest["goal"],
    goalRatePctWeek: draft.goalRatePctWeek,
    targetWeightKg: draft.targetWeightKg ? Number(draft.targetWeightKg) : undefined,
    dietaryPrefs: draft.dietaryPrefs,
    allergens: draft.allergens,
    injuries: draft.injuries,
    equipment: draft.equipment,
    trainingDaysWeek: draft.trainingDaysWeek,
    sessionMinutes: draft.sessionMinutes,
    healthScreening: draft.healthScreening,
  };
}

/** Zufaellige, plausible Werte fuer Felder, die "mit Standardwerten fertigstellen" nicht sinnvoll leer lassen kann. */
export function fillRequiredDefaults(draft: OnboardingDraft): OnboardingDraft {
  return {
    ...draft,
    birthDate: draft.birthDate || "2000-01-01",
    heightCm: draft.heightCm || "175",
    weightKg: draft.weightKg || "75",
  };
}
