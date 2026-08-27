// Client fuer Fortschritt & Auswertung (Epic Fortschritt & Auswertung, FR-130/131/132/133/137).
import { getToken, request } from "./api";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export interface WeightPoint {
  date: string;
  weightKg: number;
}

export interface WeightHistory {
  raw: WeightPoint[];
  sevenDayAverage: WeightPoint[];
}

export function fetchWeightHistory(from: string, to: string) {
  const params = new URLSearchParams({ from, to });
  return request<WeightHistory>(`/api/progress/weight?${params.toString()}`);
}

export interface DailyNutrition {
  date: string;
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
}

export interface WeeklyNutritionAverage {
  weekStart: string;
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
}

export interface NutritionHistory {
  daily: DailyNutrition[];
  weeklyAverages: WeeklyNutritionAverage[];
  adherenceRate: number;
  targetKcal: number | null;
}

export function fetchNutritionHistory(from: string, to: string) {
  const params = new URLSearchParams({ from, to });
  return request<NutritionHistory>(`/api/progress/nutrition?${params.toString()}`);
}

export interface WeeklyMuscleVolume {
  weekStart: string;
  muscle: string;
  sets: number;
  corridorMin: number;
  corridorMax: number;
}

export function fetchVolumeHistory(from: string, to: string) {
  const params = new URLSearchParams({ from, to });
  return request<WeeklyMuscleVolume[]>(`/api/progress/volume?${params.toString()}`);
}

export type Rating = "GOOD" | "NEUTRAL" | "NEEDS_ATTENTION";
export type WeeklyReportRecommendation =
  | "FOCUS_ON_TRAINING_SESSIONS"
  | "FOCUS_ON_NUTRITION_LOGGING"
  | "NUTRITION_OFF_TARGET"
  | "ON_TRACK";

export interface WeeklyReport {
  weekStart: string;
  weekEnd: string;
  sessionsCompleted: number;
  sessionsPlanned: number;
  nutritionDaysLogged: number;
  avgKcal: number | null;
  targetKcal: number | null;
  weightChangeKg: number | null;
  trainingRating: Rating;
  nutritionLoggingRating: Rating;
  nutritionTargetRating: Rating | null;
  recommendation: WeeklyReportRecommendation;
}

/** BIZ-01: Plus/Coach-Feature, Free-Tier bekommt `weekly_report_requires_upgrade` (422). */
export function fetchWeeklyReport(weekStart?: string) {
  const params = weekStart ? `?${new URLSearchParams({ weekStart }).toString()}` : "";
  return request<WeeklyReport>(`/api/progress/weekly-report${params}`);
}

export interface LoggedExercise {
  id: string;
  name: string;
}

export function fetchLoggedExercises() {
  return request<LoggedExercise[]>("/api/trainlog/exercises/logged");
}

export interface OneRepMaxPoint {
  loggedAt: string;
  estimated1Rm: number;
}

export function fetchOneRepMaxHistory(exerciseId: string) {
  return request<OneRepMaxPoint[]>(`/api/trainlog/exercises/${exerciseId}/one-rep-max-history`);
}

/** FR-137: Downloads brauchen den Bearer-Token im Header, ein einfacher `<a href>` kann das
 * nicht -- deshalb per `fetch` laden und als Blob-URL "herunterladen" lassen (Standardmuster,
 * kein externer Dienst involviert). */
async function downloadWithAuth(path: string, filename: string) {
  const token = getToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) throw new Error(`Export fehlgeschlagen (${response.status})`);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function downloadJsonExport() {
  return downloadWithAuth("/api/export/json", "onshape-export.json");
}

export function downloadCsvExport() {
  return downloadWithAuth("/api/export/csv", "onshape-export-csv.zip");
}
