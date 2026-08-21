// Client fuer das Kotlin/Spring-Backend (Epic Onboarding & Profil, FR-01..FR-11).
// Bewusst plain fetch statt Next.js Server Actions: das Backend ist ein eigener Dienst
// (nicht die Next.js-eigene DB), der Token lebt im Browser (localStorage), nicht in einer
// Next.js-Session.

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_STORAGE_KEY = "onshape.token";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.code ?? "unknown_error",
      body?.message ?? `Request fehlgeschlagen (${response.status})`,
      body?.fieldErrors ?? {},
    );
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
}

export function register(email: string, password: string, locale: string) {
  return request<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password, locale }),
  });
}

export function login(email: string, password: string) {
  return request<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

/** Spiegelt OnboardingRequest.kt (Backend). */
export interface OnboardingRequest {
  sex: "male" | "female" | "other" | "unspecified";
  birthDate: string; // ISO yyyy-MM-dd
  heightCm: number;
  weightKg: number;
  bodyFatPct?: number;
  experience: "none" | "beginner" | "intermediate" | "advanced";
  activityPal: number;
  goal: "lose" | "gain_muscle" | "gain_weight" | "strength" | "maintain" | "recomp";
  goalRatePctWeek: number;
  targetWeightKg?: number;
  dietaryPrefs: string[];
  allergens: string[];
  injuries: string[];
  equipment: string[];
  trainingDaysWeek: number;
  sessionMinutes: number;
  healthScreening: {
    heartCondition: boolean;
    pregnancy: boolean;
    recentInjury: boolean;
    medication: boolean;
  };
}

export interface OnboardingResultResponse {
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
  fiberG: number;
  waterMl: number;
  calculation: Record<string, unknown>;
  healthAdvisory: { needsMedicalAdvice: boolean; triggeredFlags: string[] };
}

export function submitOnboarding(payload: OnboardingRequest) {
  return request<OnboardingResultResponse>("/api/onboarding/profile", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function fetchOnboardingResult() {
  return request<OnboardingResultResponse>("/api/onboarding/result");
}
