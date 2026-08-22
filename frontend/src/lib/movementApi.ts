// Client fuer die Bewegungsvermittlung (Epic Bewegungsvermittlung, FR-110/111/114).
import { request } from "./api";

export interface ExerciseMistake {
  id: string;
  title: string;
  whyBad: string;
  fix: string;
  imageUrl: string | null;
  severity: number;
}

export interface ProgressionLink {
  id: string;
  slug: string;
  name: string;
}

export interface StartingWeight {
  weightKg: number | null;
  reasonCode: string;
}

export interface ExerciseDetail {
  id: string;
  slug: string;
  name: string;
  pattern: string;
  mechanic: string;
  equipment: string[];
  difficulty: string;
  primaryMuscles: string[];
  videoFrontUrl: string | null;
  videoSideUrl: string | null;
  thumbnailUrl: string | null;
  setupSteps: string[];
  executionSteps: string[];
  cues: string[];
  breathing: string | null;
  tempo: string | null;
  whatIsNormal: string | null;
  mistakes: ExerciseMistake[];
  regressionOf: ProgressionLink | null;
  progressionTo: ProgressionLink | null;
  startingWeight: StartingWeight | null;
  showBeginnerIntro: boolean;
  hasContent: boolean;
}

export function fetchExerciseDetail(exerciseId: string, locale: string) {
  const params = new URLSearchParams({ locale });
  return request<ExerciseDetail>(`/api/movement/exercises/${exerciseId}?${params.toString()}`);
}
