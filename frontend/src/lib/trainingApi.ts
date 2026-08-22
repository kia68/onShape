// Client fuer das Trainingsplan-Backend (Epic Trainingsplan-Generierung, FR-70..FR-77).
import { request } from "./api";

export type SwapReason = "too_hard" | "equipment_occupied" | "pain" | "dislike" | "other";

export interface ProgramItem {
  id: string;
  exerciseId: string;
  exerciseName: string;
  sortOrder: number;
  sets: number;
  repMin: number | null;
  repMax: number | null;
  durationMinutes: number | null;
  targetRir: number | null;
  restSeconds: number;
}

export interface ProgramDay {
  id: string;
  weekNumber: number;
  dayIndex: number;
  name: string;
  isDeload: boolean;
  items: ProgramItem[];
}

export interface Program {
  id: string;
  name: string;
  goal: string;
  daysPerWeek: number;
  weeks: number;
  splitType: string;
  generatedBy: string;
  isActive: boolean;
  days: ProgramDay[];
}

export function generateProgram(weeks: number, splitTypeOverride?: string) {
  return request<Program>("/api/training/programs/generate", {
    method: "POST",
    body: JSON.stringify({ weeks, splitTypeOverride }),
  });
}

export function fetchActiveProgram() {
  return request<Program>("/api/training/programs/active");
}

export interface SwapExerciseResponse {
  program: Program;
  replacementExerciseId: string;
  replacementExerciseName: string;
}

export function swapExercise(programId: string, exerciseId: string, reason: SwapReason) {
  return request<SwapExerciseResponse>(`/api/training/programs/${programId}/items/${exerciseId}/swap`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export type VolumeStatus = "UNDER" | "IN_RANGE" | "OVER";

export interface MuscleVolumeEntry {
  muscle: string;
  plannedSets: number;
  corridorMin: number;
  corridorMax: number;
  status: VolumeStatus;
}

export interface VolumeDashboard {
  weekNumber: number;
  isDeload: boolean;
  entries: MuscleVolumeEntry[];
}

export function fetchVolumeDashboard(week: number) {
  const params = new URLSearchParams({ week: String(week) });
  return request<VolumeDashboard>(`/api/training/programs/active/volume?${params.toString()}`);
}
