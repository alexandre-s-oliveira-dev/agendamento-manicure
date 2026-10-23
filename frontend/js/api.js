/*
  Camada de acesso aos dados.
  Hoje: USE_MOCK = true -> dados de exemplo salvos no navegador (localStorage).
  Etapa 5: troque USE_MOCK para false e ajuste BASE_URL para a API Spring Boot.
  O resto do site não muda, porque só este arquivo "fala" com os dados.
*/
class ErroApi extends Error {
  constructor(mensagem, codigo) { super(mensagem); this.codigo = codigo; }
}

const API = (() => {
  const CONFIG = { USE_MOCK: false, BASE_URL: "http://localhost:8080/api" };
  const CHAVE_TOKEN = "token_admin"; // o login da profissional fica só nesta aba (sessionStorage)

  /* ---------- utilidades ---------- */
  const paraISO = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const deISO = (s) => { const [a, m, d] = s.split("-").map(Number); return new Date(a, m - 1, d); };
  const paraMin = (h) => { const [hh, mm] = h.split(":").map(Number); return hh * 60 + mm; };
  const paraHora = (min) => `${String(Math.floor(min / 60)).padStart(2, "0")}:${String(min % 60).padStart(2, "0")}`;

  /* ---------- dados de exemplo (mock) ---------- */
  const SERVICOS_MOCK = [
    { id: 1, nome: "Manicure", descricao: "Cutilagem, lixamento e esmaltação.", preco: 35, duracaoMinutos: 45, ativo: true },
    { id: 2, nome: "Pedicure", descricao: "Cuidado completo dos pés com esmaltação.", preco: 50, duracaoMinutos: 60, ativo: true },
    { id: 3, nome: "Mão e pé", descricao: "Manicure e pedicure no mesmo horário.", preco: 80, duracaoMinutos: 105, ativo: true },
    { id: 4, nome: "Esmaltação em gel", descricao: "Brilho e durabilidade por até 3 semanas.", preco: 70, duracaoMinutos: 60, ativo: true },
    { id: 5, nome: "Alongamento em fibra", descricao: "Alongamento com acabamento natural.", preco: 140, duracaoMinutos: 120, ativo: true },
    { id: 6, nome: "Remoção de gel", descricao: "Remoção segura, sem agredir a unha.", preco: 30, duracaoMinutos: 30, ativo: true },
  ];
  // 0 = domingo ... 6 = sábado. Lista vazia = fechado. Cada item é [início, fim].
  const FUNCIONAMENTO_MOCK = {
    0: [], 1: [["09:00", "12:00"], ["14:00", "18:00"]], 2: [["09:00", "12:00"], ["14:00", "18:00"]],
    3: [["09:00", "12:00"], ["14:00", "18:00"]], 4: [["09:00", "12:00"], ["14:00", "18:00"]],
    5: [["09:00", "12:00"], ["14:00", "18:00"]], 6: [["09:00", "13:00"]],
  };
  const CHAVE = "agendamentos_mock";
  const lerAgendamentos = () => { try { return JSON.parse(localStorage.getItem(CHAVE)) || []; } catch { return []; } };
  const salvarAgendamentos = (lista) => localStorage.setItem(CHAVE, JSON.stringify(lista));

  function horariosMock(servicoId, data) {
    const servico = SERVICOS_MOCK.find((s) => s.id === servicoId);
    if (!servico) return [];
    const periodos = FUNCIONAMENTO_MOCK[deISO(data).getDay()] || [];
    const ocupados = lerAgendamentos()
      .filter((a) => a.data === data && a.status !== "CANCELADO")
      .map((a) => [paraMin(a.horario), paraMin(a.horario) + a.duracaoMinutos]);
    const agora = new Date();
    const limite = data === paraISO(agora) ? agora.getHours() * 60 + agora.getMinutes() : -1;
    const lista = [];
    for (const [ini, fim] of periodos) {
      for (let t = paraMin(ini); t + servico.duracaoMinutos <= paraMin(fim); t += 30) {
        if (t <= limite) continue;
        if (ocupados.some(([oi, of]) => t < of && t + servico.duracaoMinutos > oi)) continue;
        lista.push(paraHora(t));
      }
    }
    return lista;
  }

  /* ---------- chamadas reais (Etapa 5) ---------- */
  // autenticado: true -> envia o token de login (só as chamadas do painel usam isso).
  async function requisicao(caminho, { autenticado = false, ...opcoes } = {}) {
    const cabecalhos = { "Content-Type": "application/json" };
    const token = sessionStorage.getItem(CHAVE_TOKEN);
    if (autenticado && token) cabecalhos.Authorization = "Bearer " + token;
    let resp;
    try {
      resp = await fetch(CONFIG.BASE_URL + caminho, { ...opcoes, headers: cabecalhos });
    } catch {
      throw new ErroApi("Não foi possível conectar ao servidor. Tente novamente em instantes.", "REDE");
    }
    if (!resp.ok) {
      let corpo = {};
      try { corpo = await resp.json(); } catch { /* sem corpo */ }
      if (resp.status === 401) {
        if (autenticado) sessionStorage.removeItem(CHAVE_TOKEN);
        throw new ErroApi(corpo.mensagem || "Sessão expirada. Entre novamente.", "NAO_AUTORIZADO");
      }
      throw new ErroApi(corpo.mensagem || "Ocorreu um erro. Tente novamente.", resp.status === 409 ? "CONFLITO" : "HTTP");
    }
    return resp.status === 204 ? null : resp.json();
  }

  /* ---------- funções usadas pelo site ---------- */
  return {
    paraISO, deISO,

    /* ---- área da profissional ---- */
    async login(email, senha) {
      if (CONFIG.USE_MOCK) throw new ErroApi("O login só funciona com a API ligada (USE_MOCK = false).", "HTTP");
      return requisicao("/auth/login", { method: "POST", body: JSON.stringify({ email, senha }) });
    },
    salvarSessao(dados) {
      sessionStorage.setItem(CHAVE_TOKEN, dados.token);
      sessionStorage.setItem("nome_admin", dados.nome);
    },
    sessao() {
      const token = sessionStorage.getItem(CHAVE_TOKEN);
      return token ? { token, nome: sessionStorage.getItem("nome_admin") } : null;
    },
    sair() {
      sessionStorage.removeItem(CHAVE_TOKEN);
      sessionStorage.removeItem("nome_admin");
    },
    // Chamada protegida: requisitar("/agendamentos?data=2026-10-20")
    requisitar(caminho, opcoes = {}) {
      return requisicao(caminho, { ...opcoes, autenticado: true });
    },

    async listarServicos() {
      if (CONFIG.USE_MOCK) return SERVICOS_MOCK.filter((s) => s.ativo);
      return requisicao("/servicos?ativo=true");
    },

    // Devolve uma lista de horários livres, ex.: ["09:00", "09:30"]
    async horariosDisponiveis(servicoId, data) {
      if (CONFIG.USE_MOCK) return horariosMock(servicoId, data);
      return requisicao(`/horarios-disponiveis?servicoId=${servicoId}&data=${data}`);
    },

    // dados: { servicoId, data, horario, nomeCliente, telefone, observacao }
    async criarAgendamento(dados) {
      if (CONFIG.USE_MOCK) {
        if (!horariosMock(dados.servicoId, dados.data).includes(dados.horario)) {
          throw new ErroApi("Esse horário acabou de ser reservado. Escolha outro horário.", "CONFLITO");
        }
        const servico = SERVICOS_MOCK.find((s) => s.id === dados.servicoId);
        const lista = lerAgendamentos();
        const novo = { ...dados, id: lista.length + 1, status: "PENDENTE", duracaoMinutos: servico.duracaoMinutos };
        lista.push(novo);
        salvarAgendamentos(lista);
        return novo;
      }
      return requisicao("/agendamentos", { method: "POST", body: JSON.stringify(dados) });
    },
  };
})();
