// Client fuer das Ernaehrungstracking-Backend (Epic Ernaehrungstracking, FR-20..FR-32).
import { request } from "./api";

export type MealSlot = "breakfast" | "lunch" | "dinner" | "snack" | "pre_workout" | "post_workout";
export type EntryMethod = "search" | "barcode" | "photo" | "voice" | "recipe" | "quick_add" | "copy";

export interface ServingOption {
  id: string;
  label: string;
  grams: number;
  isDefault: boolean;
}

export interface FoodSearchResult {
  id: string;
  name: string;
  brand: string | null;
  kcalPer100g: number;
  proteinGPer100g: number;
  fatGPer100g: number;
  carbsGPer100g: number;
  source: "bls" | "usda" | "off" | "brand_verified" | "user" | "ai_estimate";
  trust: "verified" | "community" | "estimated";
  servings: ServingOption[];
  lastUsedGrams: number | null;
}

export function searchFoods(query: string, locale: string) {
  const params = new URLSearchParams({ q: query, locale });
  return request<FoodSearchResult[]>(`/api/foods/search?${params.toString()}`);
}

export interface LogEntryRequest {
  foodId?: string;
  recipeId?: string;
  loggedDate: string; // ISO yyyy-MM-dd
  slot: MealSlot;
  grams: number;
  servingId?: string;
  method: EntryMethod;
  clientId?: string;
}

export interface FoodEntry {
  id: string;
  foodId: string | null;
  recipeId: string | null;
  loggedDate: string;
  slot: MealSlot;
  grams: number;
  method: EntryMethod;
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
  micros: Record<string, number>;
  clientId: string | null;
  name?: string | null;
}

export function logEntry(entry: LogEntryRequest) {
  return request<FoodEntry>("/api/nutrition/entries", { method: "POST", body: JSON.stringify(entry) });
}

export function logEntriesBatch(entries: LogEntryRequest[]) {
  return request<FoodEntry[]>("/api/nutrition/entries/batch", { method: "POST", body: JSON.stringify(entries) });
}

export function deleteEntry(id: string) {
  return request<void>(`/api/nutrition/entries/${id}`, { method: "DELETE" });
}

export function copyEntries(fromDate: string, toDate: string, slot?: MealSlot) {
  return request<FoodEntry[]>("/api/nutrition/entries/copy", {
    method: "POST",
    body: JSON.stringify({ fromDate, toDate, slot }),
  });
}

export interface SlotSummary {
  slot: MealSlot;
  entries: FoodEntry[];
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
}

export interface DayView {
  date: string;
  slots: SlotSummary[];
  totalKcal: number;
  totalProteinG: number;
  totalFatG: number;
  totalCarbsG: number;
  totalMicros: Record<string, number>;
  waterMl: number;
  targetKcal: number | null;
  targetProteinG: number | null;
  targetFatG: number | null;
  targetCarbsG: number | null;
  targetWaterMl: number | null;
}

export function fetchDayView(date: string, locale: string) {
  const params = new URLSearchParams({ date, locale });
  return request<DayView>(`/api/nutrition/day?${params.toString()}`);
}

export interface WaterEntry {
  id: string;
  loggedDate: string;
  amountMl: number;
  clientId: string | null;
}

export interface WaterDay {
  date: string;
  totalMl: number;
  entries: WaterEntry[];
}

export function logWater(loggedDate: string, amountMl: number, clientId?: string) {
  return request<WaterEntry>("/api/nutrition/water", { method: "POST", body: JSON.stringify({ loggedDate, amountMl, clientId }) });
}

export function fetchWaterDay(date: string) {
  const params = new URLSearchParams({ date });
  return request<WaterDay>(`/api/nutrition/water?${params.toString()}`);
}

export function deleteWaterEntry(id: string) {
  return request<void>(`/api/nutrition/water/${id}`, { method: "DELETE" });
}
