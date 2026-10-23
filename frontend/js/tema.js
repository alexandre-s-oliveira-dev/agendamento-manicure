/*
  Modo escuro. Funciona assim:
  1) Se a pessoa já escolheu um tema antes, usa o escolhido.
  2) Senão, segue o tema do celular/computador (claro ou escuro).
  3) O botão ☾/☀ alterna e guarda a escolha neste navegador.
  Este arquivo roda dentro do <head> para a página já abrir no tema certo (sem "piscar").
*/
(function () {
  const CHAVE = "tema";

  function lerSalvo() {
    try { return localStorage.getItem(CHAVE); } catch (e) { return null; }
  }
  function sistemaEscuro() {
    return !!(window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches);
  }

  let tema = lerSalvo() || (sistemaEscuro() ? "escuro" : "claro");

  function aplicar() {
    document.documentElement.setAttribute("data-tema", tema);
    document.querySelectorAll("[data-alternar-tema]").forEach((b) => {
      b.textContent = tema === "escuro" ? "☀" : "☾";
      b.title = tema === "escuro" ? "Mudar para o modo claro" : "Mudar para o modo escuro";
    });
  }

  aplicar();
  document.addEventListener("DOMContentLoaded", () => {
    aplicar();
    document.querySelectorAll("[data-alternar-tema]").forEach((b) => {
      b.addEventListener("click", () => {
        tema = tema === "escuro" ? "claro" : "escuro";
        try { localStorage.setItem(CHAVE, tema); } catch (e) { /* sem armazenamento: vale só nesta visita */ }
        aplicar();
      });
    });
  });
})();
