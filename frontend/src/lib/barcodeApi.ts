// Client fuer den Barcode-Scanner (Epic Barcode-Scanner & Kaufberatung, FR-40..FR-49).
import { request } from "./api";

export interface ProductSummary {
  id: string;
  barcode: string | null;
  brand: string | null;
  name: string;
  category: string | null;
  novaGroup: number | null;
  nutriscore: string | null;
  kcalPer100g: number;
  proteinGPer100g: number;
  fatGPer100g: number;
  carbsGPer100g: number;
  sugarGPer100g: number | null;
  fiberGPer100g: number | null;
  saltGPer100g: number | null;
  allergens: string[];
  additives: string[];
  trust: "verified" | "community" | "estimated";
  defaultServingGrams: number;
}

export interface ScoreReason {
  code: string;
  params: Record<string, unknown>;
  weight: number;
}

export interface AlternativeProduct {
  product: ProductSummary;
  score: number;
  reasons: ScoreReason[];
}

export interface BarcodeScanResult {
  found: boolean;
  barcode: string;
  product: ProductSummary | null;
  score: number | null;
  allergenMatches: string[];
  dietaryPreferenceConflict: string | null;
  reasons: ScoreReason[];
  alternatives: AlternativeProduct[];
  /** BIZ-01 (§15.1): Free-Tier-Monatsdeckel erreicht -- score/reasons/alternatives sind dann
   * leer, allergenMatches/dietaryPreferenceConflict bleiben davon unberuehrt. */
  fitScoreGated: boolean;
}

export function scanBarcode(barcode: string, date: string) {
  return request<BarcodeScanResult>("/api/barcode/scan", {
    method: "POST",
    body: JSON.stringify({ barcode, date }),
  });
}

export interface ManualProductInput {
  barcode: string;
  nameDe: string;
  nameEn?: string;
  brand?: string;
  category?: string;
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
  sugarG?: number;
  fiberG?: number;
  saltG?: number;
  allergens?: string[];
  isLiquid?: boolean;
}

export function createManualProduct(input: ManualProductInput) {
  return request<{ id: string }>("/api/barcode/products", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
