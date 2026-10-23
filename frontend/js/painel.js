/* Painel da profissional: resumo, calendário e gerenciamento de agendamentos. */
(function () {
  const sessao = API.sessao();
  if (!sessao) { location.href = "login.html"; return; }

  const $ = (id) => document.getElementById(id);
  const hora = (s) => s.slice(0, 5);
  const dataBR = (iso) => iso.split("-").reverse().join("/");
  const tempo = (min) => `${Math.floor(min / 60)}h${String(min % 60).padStart(2, "0")}`;
  const ROTULO = { PENDENTE: "Pendente", CONFIRMADO: "Confirmado", CANCELADO: "Cancelado", CONCLUIDO: "Concluído" };
  let atual = null; // agendamento aberto no detalhe

  $("saudacao").textContent = `Olá, ${sessao.nome}`;
  $("btn-sair").addEventListener("click", () => { API.sair(); location.href = "login.html"; });

  function mostrarErro(texto, elemento = $("mensagem")) {
    elemento.textContent = texto;
    elemento.hidden = false;
  }
  function tratarErro(e, elemento) {
    if (e.codigo === "NAO_AUTORIZADO") { API.sair(); location.href = "login.html"; return; }
    mostrarErro(e.message, elemento);
  }

  /* ---------- abas ---------- */
  function trocarAba(nome) {
    $("mensagem").hidden = true;
    document.querySelectorAll("#abas .aba").forEach((b) => b.classList.toggle("ativa", b.dataset.aba === nome));
    ["resumo", "calendario", "servicos", "horarios", "bloqueios"].forEach((a) => { $("aba-" + a).hidden = a !== nome; });
    if (nome === "resumo") { animarProximo = true; carregarResumo(); }
    else if (nome === "calendario") PainelCalendario.recarregar();
    else PainelConfig.carregar(nome);
  }
  document.querySelectorAll("#abas .aba").forEach((b) => b.addEventListener("click", () => trocarAba(b.dataset.aba)));

  /* ---------- resumo ---------- */
  const moeda = (v) => Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

  /* Animações do resumo (a CSS já desliga o movimento para quem pediu "reduzir movimento") */
  function entrada(elemento, posicao) {
    elemento.classList.add("entra");
    elemento.style.animationDelay = `${posicao * 70}ms`;
  }

  // Faz o número "contar" de 0 até o valor final.
  function contar(elemento, final, formatar) {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || !final) return;
    const duracao = 900;
    const inicio = performance.now();
    elemento.textContent = formatar(0);
    function passo(agora) {
      const t = Math.min(1, (agora - inicio) / duracao);
      elemento.textContent = formatar(final * (1 - Math.pow(1 - t, 3)));
      if (t < 1) requestAnimationFrame(passo);
      else elemento.textContent = formatar(final);
    }
    requestAnimationFrame(passo);
  }

  function desenharReceita(r, animar) {
    const mes = new Date(r.hoje + "T00:00:00").toLocaleDateString("pt-BR", { month: "long" });
    $("titulo-mes").textContent = `Faturamento de ${mes}`;
    const realizado = Number(r.faturamentoRealizadoMes);
    const previsto = Number(r.faturamentoPrevistoMes);
    const total = realizado + previsto;
    const percentual = total > 0 ? Math.round((realizado / total) * 100) : 0;

    const destaque = el("div", "receita-card destaque");
    const valorReal = el("strong", "", moeda(realizado));
    destaque.append(el("span", "", "Realizado (concluídos)"), valorReal);
    const barra = el("div", "barra-progresso");
    const miolo = document.createElement("i");
    miolo.style.width = animar ? "0%" : `${percentual}%`;
    barra.appendChild(miolo);
    destaque.appendChild(barra);

    const outro = el("div", "receita-card");
    const valorPrev = el("strong", "", moeda(previsto));
    outro.append(el("span", "", "A realizar (pendentes e confirmados)"), valorPrev);
    $("receita").replaceChildren(destaque, outro);

    if (animar) {
      entrada(destaque, 5);
      entrada(outro, 6);
      contar(valorReal, realizado, moeda);
      contar(valorPrev, previsto, moeda);
      requestAnimationFrame(() => requestAnimationFrame(() => { miolo.style.width = `${percentual}%`; }));
    }
  }

  function desenharGrafico(semana) {
    const letras = ["D", "S", "T", "Q", "Q", "S", "S"];
    const maior = Math.max(1, ...semana.map((d) => d.quantidade));
    const hoje = API.paraISO(new Date());
    const colunas = semana.map((d) => {
      const col = el("div", "coluna" + (d.data === hoje ? " hoje" : "") + (d.quantidade === 0 ? " vazia" : ""));
      const area = el("div", "area-barra");
      const barra = el("div", "barra");
      area.appendChild(barra);
      col.append(el("span", "qtd", String(d.quantidade)), area, el("small", "", letras[new Date(d.data + "T00:00:00").getDay()]));
      requestAnimationFrame(() => requestAnimationFrame(() => { barra.style.height = `${(d.quantidade / maior) * 100}%`; }));
      return col;
    });
    const grafico = $("grafico-semana");
    grafico.replaceChildren(...colunas);
    grafico.setAttribute("aria-label", "Agendamentos da semana: " + semana.map((d) => `${letras[new Date(d.data + "T00:00:00").getDay()]} ${d.quantidade}`).join(", "));
  }

  function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto) e.textContent = texto;
    return e;
  }
  let animarProximo = true; // anima só quando a pessoa chega ao Resumo (não a cada ação)

  async function carregarResumo() {
    const animar = animarProximo;
    animarProximo = false;
    try {
      const r = await API.requisitar("/dashboard");
      $("titulo-hoje").textContent = `Hoje, ${dataBR(r.hoje)}`;
      const inteiro = (v) => String(Math.round(v));
      const horas = (v) => tempo(Math.round(v));
      const cartoes = [
        ["Agendamentos hoje", r.agendamentosHoje, inteiro], ["Clientes", r.totalClientes, inteiro],
        ["Serviços ativos", r.servicosAtivos, inteiro], ["Tempo ocupado hoje", r.minutosOcupadosHoje, horas],
        ["Tempo livre hoje", r.minutosLivresHoje, horas],
      ];
      $("stats").replaceChildren(...cartoes.map(([nome, valor, formatar], i) => {
        const d = el("div", "stat");
        const v = el("strong", "", formatar(valor));
        d.append(v, el("span", "", nome));
        if (animar) { entrada(d, i); contar(v, valor, formatar); }
        return d;
      }));
      desenharReceita(r, animar);
      desenharGrafico(r.semana);
      const ul = $("lista-proximos");
      ul.replaceChildren();
      if (!r.proximos.length) {
        const p = document.createElement("li");
        p.className = "vazio";
        p.textContent = "Nenhum agendamento próximo.";
        ul.appendChild(p);
      }
      r.proximos.forEach((a, i) => {
        const li = PainelCalendario.criarItem(a, true);
        if (animar) entrada(li, 7 + i);
        ul.appendChild(li);
      });
    } catch (e) { tratarErro(e); }
  }

  /* ---------- detalhe do agendamento ---------- */
  function linha(titulo, valor) {
    const d = document.createElement("div");
    const dt = document.createElement("dt"); dt.textContent = titulo;
    const dd = document.createElement("dd"); dd.textContent = valor;
    d.append(dt, dd);
    return d;
  }

  function abrirDetalhe(a) {
    atual = a;
    $("det-msg").hidden = true;
    $("remarcar").hidden = true;
    $("det-numero").textContent = `#${String(a.id).padStart(4, "0")}`;
    const itens = [["Cliente", a.nomeCliente], ["Telefone", a.telefone], ["Serviço", a.servicoNome],
      ["Data", dataBR(a.data)], ["Horário", `${hora(a.horaInicio)} às ${hora(a.horaFim)}`], ["Status", ROTULO[a.status] || a.status]];
    if (a.observacao) itens.push(["Observação", a.observacao]);
    $("det-info").replaceChildren(...itens.map(([t, v]) => linha(t, v)));
    const aberto = a.status === "PENDENTE" || a.status === "CONFIRMADO";
    $("btn-confirmar").hidden = a.status !== "PENDENTE";
    $("btn-concluir").hidden = a.status !== "CONFIRMADO";
    $("btn-remarcar").hidden = !aberto;
    $("btn-cancelar").hidden = !aberto;
    $("detalhe").showModal();
  }

  function atualizarTelas() {
    $("detalhe").close();
    if (!$("aba-resumo").hidden) carregarResumo(); else PainelCalendario.recarregar();
  }

  async function salvar(corpo) {
    try {
      await API.requisitar(`/agendamentos/${atual.id}`, { method: "PUT", body: JSON.stringify(corpo) });
      atualizarTelas();
    } catch (e) { tratarErro(e, $("det-msg")); }
  }

  $("btn-confirmar").addEventListener("click", () => salvar({ status: "CONFIRMADO" }));
  $("btn-concluir").addEventListener("click", () => salvar({ status: "CONCLUIDO" }));
  $("btn-cancelar").addEventListener("click", () => { if (confirm("Cancelar este agendamento?")) salvar({ status: "CANCELADO" }); });
  $("btn-fechar").addEventListener("click", () => $("detalhe").close());
  $("btn-excluir").addEventListener("click", async () => {
    if (!confirm("Excluir este agendamento de vez? Isso não pode ser desfeito.")) return;
    try {
      await API.requisitar(`/agendamentos/${atual.id}`, { method: "DELETE" });
      atualizarTelas();
    } catch (e) { tratarErro(e, $("det-msg")); }
  });

  /* ---------- remarcar ---------- */
  $("btn-remarcar").addEventListener("click", () => {
    $("remarcar").hidden = !$("remarcar").hidden;
    $("rem-data").min = API.paraISO(new Date());
    $("rem-data").value = "";
    $("rem-hora").replaceChildren();
  });

  $("rem-data").addEventListener("change", async () => {
    const select = $("rem-hora");
    select.replaceChildren();
    $("det-msg").hidden = true;
    if (!$("rem-data").value) return;
    try {
      const livres = await API.horariosDisponiveis(atual.servicoId, $("rem-data").value);
      if (!livres.length) {
        const o = document.createElement("option");
        o.textContent = "Sem horários nesse dia";
        o.value = "";
        select.appendChild(o);
        return;
      }
      livres.forEach((h) => { const o = document.createElement("option"); o.value = h; o.textContent = h; select.appendChild(o); });
    } catch (e) { mostrarErro(e.message, $("det-msg")); }
  });

  $("btn-rem-salvar").addEventListener("click", () => {
    if (!$("rem-data").value || !$("rem-hora").value) { mostrarErro("Escolha uma data e um horário disponível.", $("det-msg")); return; }
    salvar({ data: $("rem-data").value, horario: $("rem-hora").value });
  });

  PainelCalendario.iniciar(abrirDetalhe, (e) => tratarErro(e));
  PainelConfig.iniciar((e, elemento) => tratarErro(e, elemento));
  carregarResumo();
})();
