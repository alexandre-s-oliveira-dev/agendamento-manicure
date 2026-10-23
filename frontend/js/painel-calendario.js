/* Calendário do painel: visão por dia, semana ou mês. */
const PainelCalendario = (() => {
  const $ = (id) => document.getElementById(id);
  const ROTULO = { PENDENTE: "Pendente", CONFIRMADO: "Confirmado", CANCELADO: "Cancelado", CONCLUIDO: "Concluído" };
  const estado = { modo: "semana", ref: new Date(), seq: 0, aoAbrir: null, aoErro: null };
  const hora = (s) => s.slice(0, 5);
  const dataCurta = (iso) => { const [, m, d] = iso.split("-"); return `${d}/${m}`; };
  const sodia = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate());

  /* Um agendamento na lista: cliente, serviço, horário e status. */
  function criarItem(a, mostrarData) {
    const li = document.createElement("li");
    const b = document.createElement("button");
    b.type = "button";
    b.className = "item-agenda status-" + a.status.toLowerCase();
    const info = document.createElement("span");
    const titulo = document.createElement("strong");
    titulo.textContent = `${mostrarData ? dataCurta(a.data) + " · " : ""}${hora(a.horaInicio)} · ${a.nomeCliente}`;
    const sub = document.createElement("small");
    sub.textContent = a.servicoNome;
    info.append(titulo, sub);
    const selo = document.createElement("span");
    selo.className = "selo selo-" + a.status.toLowerCase();
    selo.textContent = ROTULO[a.status] || a.status;
    b.append(info, selo);
    b.addEventListener("click", () => estado.aoAbrir(a));
    li.appendChild(b);
    return li;
  }

  function lista(itens) {
    const ul = document.createElement("ul");
    ul.className = "lista-agenda";
    itens.forEach((a) => ul.appendChild(criarItem(a, false)));
    return ul;
  }

  function vazio(texto) {
    const p = document.createElement("p");
    p.className = "vazio";
    p.textContent = texto;
    return p;
  }

  function intervalo() {
    const r = sodia(estado.ref);
    if (estado.modo === "dia") return [r, r];
    if (estado.modo === "semana") {
      const ini = new Date(r); ini.setDate(r.getDate() - r.getDay());
      const fim = new Date(ini); fim.setDate(ini.getDate() + 6);
      return [ini, fim];
    }
    return [new Date(r.getFullYear(), r.getMonth(), 1), new Date(r.getFullYear(), r.getMonth() + 1, 0)];
  }

  function rotulo(ini, fim) {
    if (estado.modo === "dia") return ini.toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
    if (estado.modo === "semana") return `${dataCurta(API.paraISO(ini))} – ${dataCurta(API.paraISO(fim))}`;
    return ini.toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
  }

  function mover(direcao) {
    const r = sodia(estado.ref);
    if (estado.modo === "dia") r.setDate(r.getDate() + direcao);
    else if (estado.modo === "semana") r.setDate(r.getDate() + 7 * direcao);
    else { r.setDate(1); r.setMonth(r.getMonth() + direcao); }
    estado.ref = r;
    recarregar();
  }

  function desenharSemana(ini, porDia) {
    const frag = document.createDocumentFragment();
    for (let i = 0; i < 7; i++) {
      const dia = new Date(ini); dia.setDate(ini.getDate() + i);
      const iso = API.paraISO(dia);
      const bloco = document.createElement("section");
      bloco.className = "dia-bloco";
      const h = document.createElement("h3");
      h.textContent = dia.toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "numeric" });
      bloco.appendChild(h);
      bloco.appendChild(porDia[iso] ? lista(porDia[iso]) : vazio("Sem agendamentos."));
      frag.appendChild(bloco);
    }
    return frag;
  }

  function desenharMes(ini, fim, porDia) {
    const grade = document.createElement("div");
    grade.className = "mes-grade";
    ["D", "S", "T", "Q", "Q", "S", "S"].forEach((s) => {
      const c = document.createElement("span"); c.className = "cal-sem"; c.textContent = s; grade.appendChild(c);
    });
    for (let i = 0; i < ini.getDay(); i++) grade.appendChild(document.createElement("span"));
    const hoje = API.paraISO(new Date());
    for (let d = 1; d <= fim.getDate(); d++) {
      const dia = new Date(ini.getFullYear(), ini.getMonth(), d);
      const iso = API.paraISO(dia);
      const qtd = (porDia[iso] || []).filter((a) => a.status !== "CANCELADO").length;
      const b = document.createElement("button");
      b.type = "button";
      b.className = "mes-dia" + (iso === hoje ? " hoje" : "");
      b.textContent = d;
      if (qtd) {
        const pill = document.createElement("span");
        pill.className = "mes-qtd";
        pill.textContent = qtd;
        b.appendChild(pill);
      }
      b.addEventListener("click", () => { estado.ref = dia; estado.modo = "dia"; recarregar(); });
      grade.appendChild(b);
    }
    return grade;
  }

  async function recarregar() {
    const [ini, fim] = intervalo();
    $("cal-rotulo").textContent = rotulo(ini, fim);
    document.querySelectorAll("#cal-modos button").forEach((b) => b.classList.toggle("ativo", b.dataset.modo === estado.modo));
    const seq = ++estado.seq;
    try {
      const dados = await API.requisitar(`/agendamentos?de=${API.paraISO(ini)}&ate=${API.paraISO(fim)}`);
      if (seq !== estado.seq) return; // chegou uma resposta mais nova
      const porDia = {};
      dados.forEach((a) => { (porDia[a.data] = porDia[a.data] || []).push(a); });
      const area = $("cal-conteudo");
      if (estado.modo === "dia") area.replaceChildren(dados.length ? lista(dados) : vazio("Nenhum agendamento neste dia."));
      else if (estado.modo === "semana") area.replaceChildren(desenharSemana(ini, porDia));
      else area.replaceChildren(desenharMes(ini, fim, porDia));
    } catch (e) { estado.aoErro(e); }
  }

  function iniciar(aoAbrir, aoErro) {
    estado.aoAbrir = aoAbrir;
    estado.aoErro = aoErro;
    document.querySelectorAll("#cal-modos button").forEach((b) => b.addEventListener("click", () => { estado.modo = b.dataset.modo; recarregar(); }));
    $("cal-ant").addEventListener("click", () => mover(-1));
    $("cal-prox").addEventListener("click", () => mover(1));
    $("cal-hoje").addEventListener("click", () => { estado.ref = new Date(); recarregar(); });
  }

  return { iniciar, recarregar, criarItem };
})();
