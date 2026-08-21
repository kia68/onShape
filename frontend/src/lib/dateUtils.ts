/** yyyy-MM-dd im lokalen Zeitzonen-Kontext -- `toISOString()` waere UTC und koennte je nach
 * Tageszeit auf den falschen Kalendertag rutschen. */
export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function addDays(isoDate: string, delta: number): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() + delta);
  return toIsoDate(date);
}
