// Client fuer FR-153 (CSV-Import von Hevy/Strong).
// Bewusst NICHT ueber request() aus ./api: das setzt Content-Type: application/json fest, ein
// multipart/form-data-Upload braucht aber die vom Browser automatisch gesetzte Boundary im
// Content-Type-Header -- die darf hier nicht manuell ueberschrieben werden.
import { ApiError, getToken } from "./api";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export interface ImportSummary {
  sessionsImported: number;
  setsImported: number;
  unmatchedExercises: Record<string, number>;
  warnings: string[];
}

async function uploadCsv(path: string, file: File): Promise<ImportSummary> {
  const token = getToken();
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(response.status, body?.code ?? "unknown_error", body?.message ?? body?.error ?? `Import fehlgeschlagen (${response.status})`);
  }
  return (await response.json()) as ImportSummary;
}

export function importHevyCsv(file: File) {
  return uploadCsv("/api/import/hevy", file);
}

export function importStrongCsv(file: File) {
  return uploadCsv("/api/import/strong", file);
}
