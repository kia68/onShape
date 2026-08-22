// FR-31: Offline-Logging. Eintraege werden lokal gespeichert und synchronisieren bei
// Verbindung. Konfliktfrei durch client-generierte `clientId` (siehe V3__nutrition_log.sql
// `food_entries.client_id`, unique je Nutzer) -- ein Retry desselben Offline-Eintrags erzeugt
// serverseitig nie ein Duplikat, dieses Modul muss sich also nicht um Exactly-Once kuemmern,
// nur um "irgendwann mindestens einmal ankommen".

import { ApiError } from "./api";
import { logEntry, logWater, type LogEntryRequest } from "./nutritionApi";
import { logSet, type LogSetRequest } from "./trainlogApi";

const QUEUE_STORAGE_KEY = "onshape.offlineQueue";

type QueuedAction =
  | { kind: "food"; clientId: string; payload: LogEntryRequest }
  | { kind: "water"; clientId: string; payload: { loggedDate: string; amountMl: number } }
  | { kind: "workoutSet"; clientId: string; payload: { sessionId: string; entry: LogSetRequest } };

function readQueue(): QueuedAction[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(QUEUE_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as QueuedAction[]) : [];
  } catch {
    return [];
  }
}

function writeQueue(queue: QueuedAction[]) {
  window.localStorage.setItem(QUEUE_STORAGE_KEY, JSON.stringify(queue));
}

function newClientId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto ? crypto.randomUUID() : `offline-${Date.now()}-${Math.random()}`;
}

export function pendingCount(): number {
  return readQueue().length;
}

/** Netzwerkfehler (offline) vs. echte API-Fehlerantwort (4xx/5xx) unterscheiden -- nur
 * ersteres soll in die Offline-Queue wandern, ein 422 (z. B. Validierung) waere dort nur
 * endlos festhaengend. */
function isNetworkFailure(error: unknown): boolean {
  return !(error instanceof ApiError);
}

export async function logFoodEntryWithOfflineFallback(payload: Omit<LogEntryRequest, "clientId">): Promise<{ queued: boolean; clientId: string }> {
  const clientId = newClientId();
  try {
    await logEntry({ ...payload, clientId });
    return { queued: false, clientId };
  } catch (error) {
    if (!isNetworkFailure(error)) throw error;
    const queue = readQueue();
    queue.push({ kind: "food", clientId, payload: { ...payload, clientId } });
    writeQueue(queue);
    return { queued: true, clientId };
  }
}

export async function logWaterWithOfflineFallback(loggedDate: string, amountMl: number): Promise<{ queued: boolean; clientId: string }> {
  const clientId = newClientId();
  try {
    await logWater(loggedDate, amountMl, clientId);
    return { queued: false, clientId };
  } catch (error) {
    if (!isNetworkFailure(error)) throw error;
    const queue = readQueue();
    queue.push({ kind: "water", clientId, payload: { loggedDate, amountMl } });
    writeQueue(queue);
    return { queued: true, clientId };
  }
}

/** FR-96: Satzweises Offline-Logging waehrend eines laufenden Workouts, gleiches Idempotenz-
 * Muster wie Essen/Wasser (clientId, siehe V13__workout_sets_offline_sync.sql). */
export async function logWorkoutSetWithOfflineFallback(
  sessionId: string,
  entry: Omit<LogSetRequest, "clientId">,
): Promise<{ queued: boolean; clientId: string; result?: Awaited<ReturnType<typeof logSet>> }> {
  const clientId = newClientId();
  const payload = { ...entry, clientId };
  try {
    const result = await logSet(sessionId, payload);
    return { queued: false, clientId, result };
  } catch (error) {
    if (!isNetworkFailure(error)) throw error;
    const queue = readQueue();
    queue.push({ kind: "workoutSet", clientId, payload: { sessionId, entry: payload } });
    writeQueue(queue);
    return { queued: true, clientId };
  }
}

/** Versucht alle wartenden Eintraege zu senden. Bleibt ein Eintrag wegen (weiterhin)
 * fehlender Verbindung fehlgeschlagen, bricht der Flush ab und laesst den Rest der Queue
 * unangetastet -- die naechste Gelegenheit (naechstes `online`-Event) versucht es erneut. */
export async function flushOfflineQueue(): Promise<{ synced: number; remaining: number }> {
  const queue = readQueue();
  let synced = 0;
  const remaining: QueuedAction[] = [];

  for (const action of queue) {
    try {
      if (action.kind === "food") {
        await logEntry(action.payload);
      } else if (action.kind === "water") {
        await logWater(action.payload.loggedDate, action.payload.amountMl, action.clientId);
      } else {
        await logSet(action.payload.sessionId, action.payload.entry);
      }
      synced++;
    } catch (error) {
      if (isNetworkFailure(error)) {
        remaining.push(action, ...queue.slice(queue.indexOf(action) + 1));
        break;
      }
      // Echter Serverfehler (z. B. Food inzwischen geloescht) -- verwerfen statt endlos zu haengen.
    }
  }

  writeQueue(remaining);
  return { synced, remaining: remaining.length };
}

export function registerOfflineSync(onSynced?: (result: { synced: number; remaining: number }) => void) {
  if (typeof window === "undefined") return;
  const tryFlush = () => {
    flushOfflineQueue().then((result) => {
      if (result.synced > 0) onSynced?.(result);
    });
  };
  window.addEventListener("online", tryFlush);
  if (navigator.onLine) tryFlush();
}
