// Epic Geschaeftsmodell & Billing (#13): BIZ-01 (Tiers/Feature-Gating), BIZ-02 (Stripe),
// BIZ-03 (Lifetime-Deal). KONZEPT.md §15.

import { request } from "./api";

export type Tier = "free" | "plus" | "coach";
export type BillingPeriod = "monthly" | "yearly" | "lifetime";
export type CheckoutPlan = "plus_monthly" | "plus_yearly" | "coach_monthly" | "coach_yearly" | "lifetime";

export interface SubscriptionResponse {
  tier: Tier;
  billingPeriod: BillingPeriod | null;
  status: "active" | "canceled" | null;
  isLifetime: boolean;
  currentPeriodEnd: string | null;
}

export function fetchSubscription() {
  return request<SubscriptionResponse>("/api/billing/subscription");
}

export function startCheckout(plan: CheckoutPlan) {
  return request<{ checkoutUrl: string }>("/api/billing/checkout", {
    method: "POST",
    body: JSON.stringify({ plan }),
  });
}

export function openBillingPortal() {
  return request<{ portalUrl: string }>("/api/billing/portal", { method: "POST" });
}
