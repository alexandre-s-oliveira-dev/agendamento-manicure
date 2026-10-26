/*
  Tamanho da letra ajustável (botões A− e A+).
  Funciona assim:
  1) Guarda a escolha neste navegador (localStorage).
  2) Roda dentro do <head> para a página já abrir no tamanho certo, sem "pular".
  3) Os botões são os que têm data-letra-menos e data-letra-mais.
*/
(function () {
  const CHAVE = "tamanho_letra";
  const NIVEIS = [100, 112.5, 125, 137.5]; // % do tamanho normal; 112.5 é o padrão do site
  const PADRAO = 1;

  function lerSalvo() {
    try {
      const bruto = localStorage.getItem(CHAVE);
      if (bruto === null) return PADRAO;
      const n = Number(bruto);
      return Number.isInteger(n) && n >= 0 && n < NIVEIS.length ? n : PADRAO;
    } catch (e) { return PADRAO; }
  }

  let nivel = lerSalvo();

  function aplicar() {
    document.documentElement.style.fontSize = NIVEIS[nivel] + "%";
    const menos = document.querySelector("[data-letra-menos]");
    const mais = document.querySelector("[data-letra-mais]");
    if (menos) menos.disabled = nivel === 0;
    if (mais) mais.disabled = nivel === NIVEIS.length - 1;
  }

  function mudar(passo) {
    const novo = Math.min(NIVEIS.length - 1, Math.max(0, nivel + passo));
    if (novo === nivel) return;
    nivel = novo;
    try { localStorage.setItem(CHAVE, String(nivel)); } catch (e) { /* vale só nesta visita */ }
    aplicar();
  }

  aplicar();
  document.addEventListener("DOMContentLoaded", () => {
    aplicar();
    const menos = document.querySelector("[data-letra-menos]");
    const mais = document.querySelector("[data-letra-mais]");
    if (menos) menos.addEventListener("click", () => mudar(-1));
    if (mais) mais.addEventListener("click", () => mudar(1));
  });
})();
