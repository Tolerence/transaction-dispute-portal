const LOCALE = 'en-ZA';

export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat(LOCALE, { style: 'currency', currency }).format(amount);
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(LOCALE, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, { dateStyle: 'medium' });
}

export function humanise(value: string): string {
  const words = value.toLowerCase().split('_').join(' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
