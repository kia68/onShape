"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ApiError, login, register, setToken } from "@/lib/api";
import { Field, TextInput, ErrorNotice } from "./FormControls";

export function AccountStep({
  onAuthenticated,
  locale,
}: {
  onAuthenticated: () => void;
  locale: string;
}) {
  const t = useTranslations("Onboarding.account");
  const tErrors = useTranslations("Onboarding.errors");
  const [mode, setMode] = useState<"register" | "login">("register");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = mode === "register" ? await register(email, password, locale) : await login(email, password);
      setToken(response.token);
      onAuthenticated();
    } catch (err) {
      if (err instanceof ApiError) {
        setError(tErrors.has(err.code) ? tErrors(err.code) : err.message);
      } else {
        setError(tErrors("unknown_error"));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold">{mode === "register" ? t("title") : t("loginTitle")}</h2>
        {mode === "register" && <p className="text-sm text-muted-foreground">{t("subtitle")}</p>}
      </div>

      <ErrorNotice message={error} />

      <Field label={t("emailLabel")}>
        <TextInput
          type="email"
          required
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
      </Field>

      <Field label={t("passwordLabel")} hint={mode === "register" ? t("passwordHint") : undefined}>
        <TextInput
          type="password"
          required
          minLength={mode === "register" ? 8 : undefined}
          autoComplete={mode === "register" ? "new-password" : "current-password"}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </Field>

      <Button type="submit" disabled={loading}>
        {mode === "register" ? t("submit") : t("loginSubmit")}
      </Button>

      <button
        type="button"
        className="text-sm text-muted-foreground underline underline-offset-4"
        onClick={() => setMode(mode === "register" ? "login" : "register")}
      >
        {mode === "register" ? t("switchToLogin") : t("switchToRegister")}
      </button>
    </form>
  );
}
