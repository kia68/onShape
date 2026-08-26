// Epic Recht & Compliance (#12): LEGAL-11 (Einwilligungen, §14.1) und LEGAL-12
// (Wellbeing-Schutzmechanismen, §14.5).

import { request } from "./api";

export type ConsentPurpose = "core" | "photo_ai" | "wearable_sync" | "analytics" | "marketing";

export interface ConsentResponse {
  purpose: ConsentPurpose;
  granted: boolean;
  updatedAt: string | null;
}

/** Spiegelt ConsentsRequest.kt (Backend) -- alle fuenf Zwecke auf einmal, fuer den initialen
 * Einwilligungsschritt im Onboarding. */
export interface ConsentsRequest {
  core: boolean;
  photoAi: boolean;
  wearableSync: boolean;
  analytics: boolean;
  marketing: boolean;
}

export function fetchConsents() {
  return request<ConsentResponse[]>("/api/consents");
}

export function submitConsents(payload: ConsentsRequest) {
  return request<ConsentResponse[]>("/api/consents", { method: "PUT", body: JSON.stringify(payload) });
}

export function updateConsent(purpose: ConsentPurpose, granted: boolean) {
  return request<ConsentResponse[]>(`/api/consents/${purpose}`, {
    method: "PUT",
    body: JSON.stringify({ granted }),
  });
}

export interface WellbeingResource {
  name: string;
}

export interface GuardrailStatusResponse {
  hideCalorieDisplay: boolean;
  flags: string[];
  resources: WellbeingResource[];
}

export function fetchGuardrailStatus() {
  return request<GuardrailStatusResponse>("/api/wellbeing/guardrail-status");
}

export interface PauseStatusResponse {
  trackingPaused: boolean;
  trackingPausedAt: string | null;
}

export function fetchPauseStatus() {
  return request<PauseStatusResponse>("/api/wellbeing/pause-status");
}

export function pauseTracking() {
  return request<PauseStatusResponse>("/api/wellbeing/pause", { method: "POST" });
}

export function resumeTracking() {
  return request<PauseStatusResponse>("/api/wellbeing/resume", { method: "POST" });
}
