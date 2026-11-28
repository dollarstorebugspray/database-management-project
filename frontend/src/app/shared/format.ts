const money2 = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const money0 = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
const integer = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });
const compactFmt = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 });

export function money(value: number | null | undefined): string {
  return money2.format(Number(value ?? 0));
}

export function moneyCompact(value: number | null | undefined): string {
  const amount = Number(value ?? 0);
  return Math.abs(amount) < 10000 ? money0.format(amount) : `$${compactFmt.format(amount)}`;
}

export function axisMoney(value: number, axisMax = value): string {
  return axisMax >= 10000 ? `$${compactFmt.format(value)}` : money0.format(value);
}

export function count(value: number | null | undefined): string {
  return integer.format(Number(value ?? 0));
}

export function compact(value: number | null | undefined): string {
  const amount = Number(value ?? 0);
  return Math.abs(amount) < 10000 ? integer.format(amount) : compactFmt.format(amount);
}

export function percent(value: number | null | undefined): string {
  return `${Number(value ?? 0).toFixed(1)}%`;
}

const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function monthLabel(value: string, short = false): string {
  const [year, month] = value.split('-').map(Number);
  const name = monthNames[(month || 1) - 1];
  return short ? name : `${name} ${year}`;
}

export function isoDate(daysFromToday = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
