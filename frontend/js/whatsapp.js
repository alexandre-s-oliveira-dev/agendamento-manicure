/*
  Dados do estúdio e links de WhatsApp.
  Para trocar o nome ou o número do WhatsApp, mude SÓ aqui embaixo.
*/
const ESTUDIO = {
  nome: "Studio Thay Silva",
  whatsapp: "5599981721008", // país + DDD + número, só dígitos (55 = Brasil, 99 = DDD)
};

const WhatsApp = (() => {
  const digitos = (t) => String(t || "").replace(/\D/g, "");

  // Telefone da cliente (com DDD) -> formato internacional do Brasil. Devolve "" se não parecer um telefone.
  function numeroBR(telefone) {
    const d = digitos(telefone);
    if (d.startsWith("55") && d.length >= 12) return d;
    return d.length === 10 || d.length === 11 ? "55" + d : "";
  }

  // Endereço que abre o WhatsApp numa conversa, já com o texto escrito.
  function link(numero, texto) {
    return `https://wa.me/${numero}` + (texto ? `?text=${encodeURIComponent(texto)}` : "");
  }

  const dataLonga = (iso) => API.deISO(iso).toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  const primeiroNome = (nome) => String(nome || "").trim().split(/\s+/)[0] || "";

  return { numeroBR, link, dataLonga, primeiroNome };
})();
