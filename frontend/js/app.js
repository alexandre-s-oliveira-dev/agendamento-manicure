/* Ponto de entrada: dados do estabelecimento e lista de serviços da página inicial. */
const ESTABELECIMENTO = {
  nome: ESTUDIO.nome, // o nome e o WhatsApp ficam em js/whatsapp.js
  descricao: "Manicure e pedicure em ambiente tranquilo, com horário marcado e atendimento só para você.",
};

document.addEventListener("DOMContentLoaded", async () => {
  document.title = `${ESTABELECIMENTO.nome} — Manicure e Pedicure`;
  document.getElementById("marca-nome").textContent = ESTABELECIMENTO.nome;
  document.getElementById("rodape-nome").textContent = ESTABELECIMENTO.nome;
  document.getElementById("descricao").textContent = ESTABELECIMENTO.descricao;
  const botaoWhats = document.getElementById("btn-whatsapp-direto");
  if (botaoWhats) botaoWhats.href = WhatsApp.link(ESTUDIO.whatsapp, "Olá! Gostaria de agendar um horário.");

  const lista = document.getElementById("lista-servicos");

  // Animação de entrada ao rolar. Se o navegador não suporta, ou a pessoa pediu "reduzir movimento",
  // os cartões simplesmente aparecem normais.
  const animar = "IntersectionObserver" in window && !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const observador = animar
    ? new IntersectionObserver((entradas) => entradas.forEach((e) => {
        if (e.isIntersecting) { e.target.classList.add("visivel"); observador.unobserve(e.target); }
      }), { threshold: 0.15 })
    : null;
  try {
    const servicos = await API.listarServicos();
    servicos.forEach((s, i) => {
      const card = document.createElement("article");
      card.className = "cartao";
      card.style.setProperty("--cor", CORES[i % CORES.length]);
      card.innerHTML = '<span class="amostra"></span><div><h3></h3><p></p><p class="meta"></p></div>';
      card.querySelector("h3").textContent = s.nome;
      card.querySelector("p").textContent = s.descricao;
      card.querySelector(".meta").textContent = `${Agendamento.moeda(s.preco)} · ${Agendamento.duracao(s.duracaoMinutos)}`;
      lista.appendChild(card);
      if (observador) {
        card.classList.add("revelar");
        card.style.transitionDelay = `${(i % 3) * 90}ms`; // um depois do outro
        observador.observe(card);
      }
    });
    Agendamento.iniciar(servicos);
  } catch (e) {
    lista.innerHTML = '<p class="vazio">Não foi possível carregar os serviços agora. Tente novamente em instantes.</p>';
  }
});
