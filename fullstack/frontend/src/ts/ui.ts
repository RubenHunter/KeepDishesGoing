export function setAppContent(html: string) {
    const el = document.getElementById("app");
    if (el) el.innerHTML = html;
}

export function showToast(message: string) {
    const t = document.createElement("div");
    t.className = "toast";
    t.innerText = message;
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 2400);
}

export function money(amount: number, currency = "EUR") {
    try {
        return new Intl.NumberFormat(undefined, { style: "currency", currency }).format(amount);
    } catch {
        return `${amount.toFixed(2)} ${currency}`;
    }
}

export function spinner(size = 22) {
    return `
    <svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" style="opacity:.9">
      <path d="M12 2a10 10 0 1 0 10 10" stroke="url(#g)" stroke-width="3" stroke-linecap="round"/>
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="24" y2="0" gradientUnits="userSpaceOnUse">
          <stop stop-color="#7c5cff"/><stop offset="1" stop-color="#00d4ff"/>
        </linearGradient>
      </defs>
    </svg>
  `;
}

export function el<K extends keyof HTMLElementTagNameMap>(tag: K, className?: string, html?: string) {
    const e = document.createElement(tag);
    if (className) e.className = className;
    if (html !== undefined) e.innerHTML = html;
    return e;
}

