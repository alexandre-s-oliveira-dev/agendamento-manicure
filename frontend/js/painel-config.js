/* Painel: serviços, horário de funcionamento e bloqueios. */
const PainelConfig = (() => {
  const $ = (id) => document.getElementById(id);
  const DIAS = [["MONDAY", "Segunda-feira"], ["TUESDAY", "Terça-feira"], ["WEDNESDAY", "Quarta-feira"],
    ["THURSDAY", "Quinta-feira"], ["FRIDAY", "Sexta-feira"], ["SATURDAY", "Sábado"], ["SUNDAY", "Domingo"]];
  const moeda = (v) => Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  const duracao = (m) => (m >= 60 ? `${Math.floor(m / 60)}h${m % 60 ? String(m % 60).padStart(2, "0") : ""}` : `${m} min`);
  const dataBR = (iso) => iso.split("-").reverse().join("/");
  const hora = (s) => s.slice(0, 5);
  let aoErro = () => {};
  let servicoEditando = null; // null = criando um novo

  function msg(el, texto, ok = false) { el.textContent = texto; el.className = "mensagem" + (ok ? " ok" : ""); el.hidden = false; }
  function el(tag, classe, texto) { const e = document.createElement(tag); if (classe) e.className = classe; if (texto) e.textContent = texto; return e; }
  function botao(texto, classe, aoClicar) {
    const b = el("button", "btn btn-pequeno " + classe, texto);
    b.type = "button";
    b.addEventListener("click", aoClicar);
    return b;
  }

  /* ================= SERVIÇOS ================= */
  async function carregarServicos() {
    try {
      const dados = await API.requisitar("/servicos");
      const ul = $("lista-servicos-admin");
      ul.replaceChildren();
      if (!dados.length) ul.appendChild(el("li", "vazio", "Nenhum serviço cadastrado."));
      dados.forEach((s) => {
        const li = el("li", "item-agenda" + (s.ativo ? "" : " servico-inativo"));
        const info = el("span");
        info.append(el("strong", "", s.nome), el("small", "", `${moeda(s.preco)} · ${duracao(s.duracaoMinutos)}${s.ativo ? "" : " · Desativado"}`));
        const acoes = el("div", "acoes-item");
        acoes.append(
          botao("Editar", "btn-sec", () => abrirServico(s)),
          botao(s.ativo ? "Desativar" : "Ativar", "btn-sec", () => alternarServico(s)),
          botao("Excluir", "btn-sec", () => excluirServico(s)));
        li.append(info, acoes);
        ul.appendChild(li);
      });
    } catch (e) { aoErro(e); }
  }

  function corpoServico(s, ativo) {
    return { nome: s.nome, descricao: s.descricao, preco: s.preco, duracaoMinutos: s.duracaoMinutos, ativo };
  }

  async function alternarServico(s) {
    try {
      await API.requisitar(`/servicos/${s.id}`, { method: "PUT", body: JSON.stringify(corpoServico(s, !s.ativo)) });
      carregarServicos();
    } catch (e) { aoErro(e); }
  }

  async function excluirServico(s) {
    if (!confirm(`Excluir o serviço "${s.nome}"?`)) return;
    try {
      await API.requisitar(`/servicos/${s.id}`, { method: "DELETE" });
      carregarServicos();
    } catch (e) {
      if (e.codigo === "CONFLITO") aoErro(new ErroApi("Esse serviço já tem agendamentos e não pode ser excluído. Desative-o para ele deixar de aparecer no site.", "HTTP"));
      else aoErro(e);
    }
  }

  function abrirServico(s) {
    servicoEditando = s || null;
    $("svc-titulo").textContent = s ? "Editar serviço" : "Novo serviço";
    $("svc-nome").value = s ? s.nome : "";
    $("svc-desc").value = s && s.descricao ? s.descricao : "";
    $("svc-preco").value = s ? s.preco : "";
    $("svc-dur").value = s ? s.duracaoMinutos : "";
    $("svc-ativo").checked = s ? s.ativo : true;
    $("svc-msg").hidden = true;
    $("dlg-servico").showModal();
  }

  async function salvarServico() {
    const nome = $("svc-nome").value.trim();
    const preco = parseFloat($("svc-preco").value);
    const dur = parseInt($("svc-dur").value, 10);
    if (!nome) return msg($("svc-msg"), "Informe o nome do serviço.");
    if (isNaN(preco) || preco < 0) return msg($("svc-msg"), "Informe um preço válido.");
    if (isNaN(dur) || dur < 5) return msg($("svc-msg"), "A duração mínima é de 5 minutos.");
    const corpo = { nome, descricao: $("svc-desc").value.trim(), preco, duracaoMinutos: dur, ativo: $("svc-ativo").checked };
    try {
      if (servicoEditando) await API.requisitar(`/servicos/${servicoEditando.id}`, { method: "PUT", body: JSON.stringify(corpo) });
      else await API.requisitar("/servicos", { method: "POST", body: JSON.stringify(corpo) });
      $("dlg-servico").close();
      carregarServicos();
    } catch (e) { aoErro(e, $("svc-msg")); }
  }

  /* ================= HORÁRIOS DE FUNCIONAMENTO ================= */
  function linhaPeriodo(container, ini, fim) {
    const l = el("div", "periodo");
    const a = el("input"); a.type = "time"; a.value = ini;
    const b = el("input"); b.type = "time"; b.value = fim;
    const x = botao("✕", "btn-sec", () => l.remove());
    x.setAttribute("aria-label", "Remover período");
    l.append(a, el("span", "", "às"), b, x);
    container.appendChild(l);
  }

  function cartaoDia(chave, nome, periodos) {
    const card = el("div", "cartao-dia");
    const topo = el("label", "check");
    const aberto = el("input"); aberto.type = "checkbox"; aberto.checked = periodos.length > 0;
    topo.append(aberto, el("strong", "", nome));
    const area = el("div", "area-periodos");
    const linhas = el("div", "periodos");
    periodos.forEach((p) => linhaPeriodo(linhas, hora(p.horaInicio), hora(p.horaFim)));
    const retorno = el("p", "mensagem"); retorno.hidden = true;
    const rodape = el("div", "det-acoes");
    rodape.append(
      botao("+ Adicionar período", "btn-sec", () => linhaPeriodo(linhas, "14:00", "18:00")),
      botao("Salvar " + nome.toLowerCase(), "", salvarDia));
    area.append(linhas, rodape);
    area.hidden = !aberto.checked;
    aberto.addEventListener("change", () => {
      area.hidden = !aberto.checked;
      if (aberto.checked && !linhas.children.length) linhaPeriodo(linhas, "09:00", "12:00");
    });

    async function salvarDia() {
      let lista = [];
      if (aberto.checked) {
        lista = [...linhas.querySelectorAll(".periodo")].map((l) => {
          const [a, b] = l.querySelectorAll("input");
          return { horaInicio: a.value, horaFim: b.value };
        });
        if (!lista.length || lista.some((p) => !p.horaInicio || !p.horaFim)) return msg(retorno, "Preencha o horário inicial e o final de cada período.");
      }
      try {
        await API.requisitar(`/horarios/dia/${chave}`, { method: "PUT", body: JSON.stringify({ periodos: lista }) });
        msg(retorno, "Salvo!", true);
      } catch (e) { aoErro(e, retorno); }
    }
    card.append(topo, area, retorno);
    return card;
  }

  async function carregarHorarios() {
    try {
      const dados = await API.requisitar("/horarios");
      $("horarios-dias").replaceChildren(...DIAS.map(([chave, nome]) =>
        cartaoDia(chave, nome, dados.filter((h) => h.diaSemana === chave && h.ativo))));
    } catch (e) { aoErro(e); }
  }

  /* ================= BLOQUEIOS ================= */
  async function carregarBloqueios() {
    try {
      const dados = await API.requisitar("/bloqueios");
      const hoje = API.paraISO(new Date());
      const ul = $("lista-bloqueios");
      ul.replaceChildren();
      const futuros = dados.filter((b) => b.data >= hoje);
      if (!futuros.length) ul.appendChild(el("li", "vazio", "Nenhum bloqueio marcado."));
      futuros.forEach((b) => {
        const li = el("li", "item-agenda");
        const info = el("span");
        const quando = b.diaInteiro ? "Dia inteiro" : `${hora(b.horaInicio)} às ${hora(b.horaFim)}`;
        info.append(el("strong", "", `${dataBR(b.data)} · ${quando}`), el("small", "", b.motivo || "Sem motivo informado"));
        li.append(info, botao("Remover", "btn-sec", () => removerBloqueio(b)));
        ul.appendChild(li);
      });
    } catch (e) { aoErro(e); }
  }

  async function removerBloqueio(b) {
    if (!confirm("Remover este bloqueio? Os horários voltam a ficar livres.")) return;
    try {
      await API.requisitar(`/bloqueios/${b.id}`, { method: "DELETE" });
      carregarBloqueios();
    } catch (e) { aoErro(e); }
  }

  async function bloquear() {
    const saida = $("bloq-msg");
    const data = $("bloq-data").value;
    const inteiro = $("bloq-dia").checked;
    const ini = $("bloq-ini").value, fim = $("bloq-fim").value;
    if (!data) return msg(saida, "Escolha a data.");
    if (!inteiro && (!ini || !fim)) return msg(saida, "Informe o horário inicial e o final.");
    if (!inteiro && fim <= ini) return msg(saida, "O horário final deve ser depois do inicial.");
    const corpo = { data, horaInicio: inteiro ? null : ini, horaFim: inteiro ? null : fim, motivo: $("bloq-motivo").value.trim() };
    try {
      await API.requisitar("/bloqueios", { method: "POST", body: JSON.stringify(corpo) });
      // Avisa se já havia agendamentos nesse período (eles NÃO são cancelados sozinhos).
      const dia = await API.requisitar(`/agendamentos?data=${data}`);
      const afetados = dia.filter((a) => a.status !== "CANCELADO" && (inteiro || (ini < hora(a.horaFim) && fim > hora(a.horaInicio))));
      if (afetados.length) msg(saida, `Bloqueio salvo. Atenção: já existem ${afetados.length} agendamento(s) nesse período. Eles não foram cancelados: veja no Calendário e avise as clientes.`);
      else msg(saida, "Bloqueio salvo!", true);
      $("bloq-motivo").value = "";
      carregarBloqueios();
    } catch (e) { aoErro(e, saida); }
  }

  /* ================= início ================= */
  function iniciar(funcaoErro) {
    aoErro = funcaoErro;
    $("btn-novo-servico").addEventListener("click", () => abrirServico(null));
    $("svc-salvar").addEventListener("click", salvarServico);
    $("svc-cancelar").addEventListener("click", () => $("dlg-servico").close());
    $("bloq-dia").addEventListener("change", () => { $("bloq-horas").hidden = $("bloq-dia").checked; });
    $("bloq-data").min = API.paraISO(new Date());
    $("btn-bloquear").addEventListener("click", bloquear);
  }

  function carregar(nome) {
    if (nome === "servicos") carregarServicos();
    else if (nome === "horarios") carregarHorarios();
    else if (nome === "bloqueios") carregarBloqueios();
  }

  return { iniciar, carregar };
})();
