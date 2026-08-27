// Client fuer das Trainings-Logging-Backend (Epic Trainings-Logging, FR-90..FR-98).
import { request } from "./api";

export interface WorkoutSession {
  id: string;
  programDayId: string | null;
  startedAt: string;
  finishedAt: string | null;
  perceivedEffort: number | null;
  notes: string | null;
}

export function startSession(programDayId: string | null, clientId?: string) {
  return request<WorkoutSession>("/api/trainlog/sessions", {
    method: "POST",
    body: JSON.stringify({ programDayId, clientId }),
  });
}

export function fetchActiveSession() {
  return request<WorkoutSession>("/api/trainlog/sessions/active");
}

export function finishSession(id: string, perceivedEffort?: number, notes?: string) {
  return request<WorkoutSession>(`/api/trainlog/sessions/${id}/finish`, {
    method: "PUT",
    body: JSON.stringify({ perceivedEffort, notes }),
  });
}

export interface WorkoutSet {
  id: string;
  exerciseId: string;
  setIndex: number;
  weightKg: number | null;
  reps: number | null;
  durationSec: number | null;
  distanceM: number | null;
  rir: number | null;
  isWarmup: boolean;
  completed: boolean;
  loggedAt: string;
}

export interface WorkoutSessionDetail {
  session: WorkoutSession;
  sets: WorkoutSet[];
}

export function fetchSessionDetail(id: string) {
  return request<WorkoutSessionDetail>(`/api/trainlog/sessions/${id}`);
}

export interface LogSetRequest {
  exerciseId: string;
  setIndex: number;
  weightKg?: number;
  reps?: number;
  durationSec?: number;
  distanceM?: number;
  rir?: number;
  isWarmup?: boolean;
  completed?: boolean;
  clientId?: string;
}

export type PersonalRecordType = "MAX_WEIGHT" | "MAX_REPS_AT_WEIGHT" | "EST_1RM" | "VOLUME";

export interface PersonalRecord {
  type: PersonalRecordType;
  previousBest: number | null;
  newValue: number;
}

export interface LogSetResponse {
  set: WorkoutSet;
  personalRecords: PersonalRecord[];
}

export function logSet(sessionId: string, entry: LogSetRequest) {
  return request<LogSetResponse>(`/api/trainlog/sessions/${sessionId}/sets`, {
    method: "POST",
    body: JSON.stringify(entry),
  });
}

export interface WarmupSet {
  weightKg: number;
  reps: number;
}

export interface PrefillSuggestion {
  lastWeightKg: number | null;
  lastReps: number | null;
  lastRir: number | null;
  suggestedWeightKg: number | null;
  suggestedReps: number | null;
  /** FR-94, aus dem heute vorgeschlagenen Arbeitsgewicht berechnet. */
  warmupSets: WarmupSet[];
}

export function fetchPrefill(exerciseId: string, repMax?: number, targetRir?: number) {
  const params = new URLSearchParams();
  if (repMax != null) params.set("repMax", String(repMax));
  if (targetRir != null) params.set("targetRir", String(targetRir));
  const qs = params.toString();
  return request<PrefillSuggestion>(`/api/trainlog/exercises/${exerciseId}/prefill${qs ? `?${qs}` : ""}`);
}
