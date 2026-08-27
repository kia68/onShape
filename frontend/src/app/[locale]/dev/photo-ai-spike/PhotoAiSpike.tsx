"use client";

import { useCallback, useRef, useState } from "react";
import type { PreTrainedModel, Processor, ProgressInfo } from "@huggingface/transformers";
import { Button } from "@/components/ui/button";

/**
 * Technischer Spike fuer Epic #5 (KI-Fotoerkennung), KEIN Produktfeature -- nicht verlinkt,
 * nicht i18n-abgedeckt (bewusste Ausnahme von der sonstigen next-intl-Konvention, siehe
 * BarcodeScanPage.tsx zum Vergleich). Fragestellung: laesst sich ein Vision-Language-Modell
 * komplett im Browser via WebGPU ausfuehren (transformers.js), ohne Cloud-Anbindung (LEGAL-09)
 * und ohne natives SDK (LEGAL-10)? Bisher war Epic #5 in jedem Durchgang mit "keine Vision-KI
 * vorhanden" vertagt -- 2026 gibt es on-device VLMs (siehe Recherche-Notiz zu diesem Spike),
 * aber die native-SDK-Abhaengigkeit der meisten Frameworks kollidiert mit der PWA-Architektur
 * (INFRA-04/05). WebGPU-In-Browser-Inferenz ist der einzige Pfad, der OHNE die LEGAL-10-
 * Entscheidung (PWA vs. Capacitor) auskommt -- dieser Spike misst, ob das technisch traegt
 * (Modellgroesse, Ladezeit, Inferenzgeschwindigkeit, Ausgabequalitaet), bevor ueberhaupt an
 * FR-60..FR-66 gearbeitet wird. Modell: HuggingFaceTB/SmolVLM-256M-Instruct (kleinstes
 * verfuegbare VLM, um das im Recherche-Memo genannte Multi-GB-Download-Risiko zu minimieren).
 *
 * `@huggingface/transformers` wird per dynamischem Import erst im Klick-Handler geladen, nie
 * beim Server-Render -- die Bibliothek greift auf `navigator`/WebGPU zu, das existiert serverseitig
 * nicht. Kamera-Zugriff (wie in BarcodeScanner.tsx) ist hier bewusst NICHT eingebaut: ein
 * Datei-Upload beantwortet dieselbe technische Frage ohne Permission-Prompt-Abhaengigkeit.
 */

type ModelStatus = "idle" | "loading" | "ready" | "error";
type RunStatus = "idle" | "running" | "done" | "error";

const MODEL_ID = "HuggingFaceTB/SmolVLM-256M-Instruct";
const DEFAULT_PROMPT = "List the food items visible in this image and estimate their portion size.";

export function PhotoAiSpike() {
  const [gpuSupported, setGpuSupported] = useState<boolean | null>(null);
  const [modelStatus, setModelStatus] = useState<ModelStatus>("idle");
  const [modelError, setModelError] = useState<string | null>(null);
  const [loadProgressPct, setLoadProgressPct] = useState(0);
  const [loadedBytes, setLoadedBytes] = useState({ loaded: 0, total: 0 });
  const [loadMs, setLoadMs] = useState<number | null>(null);

  const [imageDataUrl, setImageDataUrl] = useState<string | null>(null);
  const [prompt, setPrompt] = useState(DEFAULT_PROMPT);
  const [runStatus, setRunStatus] = useState<RunStatus>("idle");
  const [outputText, setOutputText] = useState("");
  const [inferenceMs, setInferenceMs] = useState<number | null>(null);
  const [runError, setRunError] = useState<string | null>(null);

  const processorRef = useRef<Processor | null>(null);
  const modelRef = useRef<PreTrainedModel | null>(null);

  const checkWebGpu = useCallback(() => {
    setGpuSupported(typeof navigator !== "undefined" && "gpu" in navigator);
  }, []);

  const loadModel = useCallback(async () => {
    setModelStatus("loading");
    setModelError(null);
    setLoadProgressPct(0);
    const start = performance.now();
    try {
      const { AutoProcessor, AutoModelForVision2Seq } = await import("@huggingface/transformers");
      const progress_callback = (info: ProgressInfo) => {
        if (info.status === "progress_total") {
          setLoadProgressPct(Math.round(info.progress));
          setLoadedBytes({ loaded: info.loaded, total: info.total });
        }
      };
      const [processor, model] = await Promise.all([
        AutoProcessor.from_pretrained(MODEL_ID, { progress_callback }),
        AutoModelForVision2Seq.from_pretrained(MODEL_ID, { dtype: "fp32", device: "webgpu", progress_callback }),
      ]);
      processorRef.current = processor;
      modelRef.current = model;
      setLoadMs(Math.round(performance.now() - start));
      setModelStatus("ready");
    } catch (error) {
      setModelError(error instanceof Error ? error.message : String(error));
      setModelStatus("error");
    }
  }, []);

  const onFileSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setImageDataUrl(reader.result as string);
    reader.readAsDataURL(file);
  };

  const runInference = useCallback(async () => {
    if (!processorRef.current || !modelRef.current || !imageDataUrl) return;
    setRunStatus("running");
    setRunError(null);
    setOutputText("");
    const start = performance.now();
    try {
      const { TextStreamer, load_image } = await import("@huggingface/transformers");
      const processor = processorRef.current;
      const model = modelRef.current;

      const image = await load_image(imageDataUrl);
      const messages = [
        { role: "user", content: [{ type: "image" }, { type: "text", text: prompt }] },
      ];
      const text = processor.apply_chat_template(messages, { add_generation_prompt: true });
      const inputs = await processor(text, image, { do_image_splitting: false });

      const streamer = new TextStreamer(processor.tokenizer!, {
        skip_prompt: true,
        skip_special_tokens: true,
        callback_function: (chunk: string) => setOutputText((prev) => prev + chunk),
      });

      await model.generate({
        ...inputs,
        do_sample: false,
        repetition_penalty: 1.1,
        max_new_tokens: 256,
        streamer,
      });

      setInferenceMs(Math.round(performance.now() - start));
      setRunStatus("done");
    } catch (error) {
      setRunError(error instanceof Error ? error.message : String(error));
      setRunStatus("error");
    }
  }, [imageDataUrl, prompt]);

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-6 p-6">
      <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm">
        <p className="font-semibold">Interner Technik-Spike -- kein Produktfeature.</p>
        <p className="text-muted-foreground">
          Testet In-Browser-VLM-Inferenz via WebGPU (transformers.js, Modell {MODEL_ID}) als
          moeglichen Pfad fuer Epic #5 (KI-Fotoerkennung), OHNE die offene LEGAL-10-Entscheidung
          (PWA vs. native App) abzuwarten. Nicht i18n-abgedeckt, nicht verlinkt.
        </p>
      </div>

      <section className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold">1. WebGPU-Verfuegbarkeit</h2>
        <Button onClick={checkWebGpu} variant="outline" size="sm">Pruefen</Button>
        {gpuSupported !== null && (
          <p className="text-sm">
            {gpuSupported ? "navigator.gpu ist verfuegbar." : "navigator.gpu ist NICHT verfuegbar -- WebGPU wird dieser Browser/dieses Geraet nicht unterstuetzen."}
          </p>
        )}
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold">2. Modell laden</h2>
        <Button onClick={loadModel} disabled={modelStatus === "loading" || modelStatus === "ready"} size="sm">
          {modelStatus === "ready" ? "Modell geladen" : modelStatus === "loading" ? "Laedt..." : "Modell laden"}
        </Button>
        {modelStatus === "loading" && (
          <p className="text-sm text-muted-foreground">
            {loadProgressPct}% ({(loadedBytes.loaded / 1_000_000).toFixed(1)} MB / {(loadedBytes.total / 1_000_000).toFixed(1)} MB)
          </p>
        )}
        {modelStatus === "ready" && loadMs !== null && (
          <p className="text-sm text-muted-foreground">Geladen in {(loadMs / 1000).toFixed(1)} s.</p>
        )}
        {modelStatus === "error" && <p className="text-sm text-destructive">Fehler: {modelError}</p>}
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold">3. Foto + Prompt</h2>
        <input type="file" accept="image/*" onChange={onFileSelected} className="text-sm" />
        {imageDataUrl && (
          // eslint-disable-next-line @next/next/no-img-element -- Spike-Vorschau, kein next/image noetig.
          <img src={imageDataUrl} alt="Ausgewaehltes Foto" className="max-h-64 w-auto rounded-md border border-input" />
        )}
        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          rows={2}
          className="rounded-md border border-input bg-background p-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-lg font-semibold">4. Analysieren</h2>
        <Button
          onClick={runInference}
          disabled={modelStatus !== "ready" || !imageDataUrl || runStatus === "running"}
          size="sm"
        >
          {runStatus === "running" ? "Laeuft..." : "Analysieren"}
        </Button>
        {outputText && (
          <pre className="whitespace-pre-wrap rounded-md border border-input bg-muted p-3 text-sm">{outputText}</pre>
        )}
        {runStatus === "done" && inferenceMs !== null && (
          <p className="text-sm text-muted-foreground">Inferenz: {(inferenceMs / 1000).toFixed(1)} s.</p>
        )}
        {runStatus === "error" && <p className="text-sm text-destructive">Fehler: {runError}</p>}
      </section>
    </div>
  );
}
