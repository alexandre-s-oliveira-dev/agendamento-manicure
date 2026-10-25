/* Fluxo de agendamento: serviço -> data -> horário -> dados -> confirmação -> sucesso. */
const Agendamento = (() => {
  const estado = { etapa: 1, servicos: [], servico: null, data: null, horario: null, calendario: null };
  const $ = (id) => document.getElementById(id);
  const moeda = (v) => v.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
  const dataLonga = (iso) => API.deISO(iso).toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  const duracao = (min) => (min >= 60 ? `${Math.floor(min / 60)}h${min % 60 ? String(min % 60).padStart(2, "0") : ""}` : `${min} min`);

  function mostrarMensagem(texto) { const m = $("mensagem"); m.textContent = texto; m.hidden = false; }
  function limparMensagem() { $("mensagem").hidden = true; }

  function irParaEtapa(n) {
    document.querySelector(".painel").classList.toggle("voltando", n < estado.etapa);
    estado.etapa = n;
    limparMensagem();
    document.querySelectorAll(".etapa").forEach((e) => { e.hidden = Number(e.dataset.etapa) !== n; });
    document.querySelectorAll("#passos li").forEach((li, i) => {
      li.classList.toggle("ativo", i + 1 === n);
      li.classList.toggle("feito", i + 1 < n);
    });
    $("acoes").hidden = n === 6;
    $("btn-voltar").hidden = n === 1;
    $("btn-avancar").hidden = n <= 3;
    $("btn-avancar").textContent = n === 5 ? "Confirmar agendamento" : "Continuar";
  }

  function selecionarServico(id) {
    estado.servico = estado.servicos.find((s) => s.id === id);
    estado.data = null; estado.horario = null;
    document.querySelectorAll(".opcao").forEach((o) => o.classList.toggle("sel", Number(o.dataset.id) === id));
    estado.calendario = new Calendario($("calendario"), { aoEscolher: escolherData });
    irParaEtapa(2);
    $("agendar").scrollIntoView();
  }

  async function escolherData(iso) {
    estado.data = iso; estado.horario = null;
    irParaEtapa(3);
    $("resumo-data").textContent = `${estado.servico.nome}, ${dataLonga(iso)}`;
    const caixa = $("horarios");
    caixa.innerHTML = '<p class="vazio">Buscando horários…</p>';
    try {
      const livres = await API.horariosDisponiveis(estado.servico.id, iso);
      caixa.innerHTML = "";
      if (!livres.length) {
        caixa.innerHTML = '<p class="vazio">Esse serviço não está disponível nessa data. Escolha outro dia.</p>';
        return;
      }
      livres.forEach((h) => {
        const b = document.createElement("button");
        b.type = "button"; b.className = "horario"; b.textContent = h;
        b.addEventListener("click", () => { estado.horario = h; irParaEtapa(4); });
        caixa.appendChild(b);
      });
    } catch (e) { caixa.innerHTML = ""; mostrarMensagem(e.message); }
  }

  function lerDados() {
    return { nome: $("nome").value.trim(), telefone: $("telefone").value.trim(), observacao: $("observacao").value.trim() };
  }

  function validarDados() {
    const { nome, telefone } = lerDados();
    const digitos = telefone.replace(/\D/g, "");
    if (!nome || !telefone) return "Preencha seu nome e telefone.";
    if (digitos.length < 10 || digitos.length > 11) return "Informe um telefone válido com DDD.";
    return null;
  }

  function linhas(lista) {
    return lista.map(([t, v]) => {
      const d = document.createElement("div");
      const dt = document.createElement("dt"); dt.textContent = t;
      const dd = document.createElement("dd"); dd.textContent = v;
      d.append(dt, dd); return d;
    });
  }

  function montarRevisao() {
    const d = lerDados(), s = estado.servico;
    const itens = [["Serviço", `${s.nome} (${duracao(s.duracaoMinutos)})`], ["Data", dataLonga(estado.data)], ["Horário", estado.horario],
      ["Valor", moeda(s.preco)], ["Nome", d.nome], ["Telefone", d.telefone]];
    if (d.observacao) itens.push(["Observação", d.observacao]);
    $("revisao").replaceChildren(...linhas(itens));
  }

  async function confirmar() {
    const d = lerDados(), btn = $("btn-avancar");
    btn.disabled = true; btn.textContent = "Confirmando…";
    try {
      const ag = await API.criarAgendamento({
        servicoId: estado.servico.id, data: estado.data, horario: estado.horario,
        nomeCliente: d.nome, telefone: d.telefone, observacao: d.observacao,
      });
      $("sucesso").replaceChildren(...linhas([
        ["Número", `#${String(ag.id).padStart(4, "0")}`], ["Nome", d.nome], ["Serviço", estado.servico.nome],
        ["Data", dataLonga(estado.data)], ["Horário", estado.horario], ["Telefone", d.telefone]]));
      const numero = `#${String(ag.id).padStart(4, "0")}`;
      const textoWhats = `Olá! Acabei de agendar pelo site.\n\nServiço: ${estado.servico.nome}\nData: ${dataLonga(estado.data)}\nHorário: ${estado.horario}\nNome: ${d.nome}\nTelefone: ${d.telefone}\nAgendamento: ${numero}`;
      $("btn-avisar-whats").href = WhatsApp.link(ESTUDIO.whatsapp, textoWhats);
      irParaEtapa(6);
      $("agendar").scrollIntoView();
    } catch (e) {
      if (e.codigo === "CONFLITO") { await escolherData(estado.data); mostrarMensagem(e.message); }
      else mostrarMensagem(e.message);
    } finally { btn.disabled = false; if (estado.etapa === 5) btn.textContent = "Confirmar agendamento"; }
  }

  function avancar() {
    if (estado.etapa === 4) {
      const erro = validarDados();
      if (erro) return mostrarMensagem(erro);
      montarRevisao(); return irParaEtapa(5);
    }
    if (estado.etapa === 5) return confirmar();
  }

  function voltar() { if (estado.etapa > 1) irParaEtapa(estado.etapa === 6 ? 1 : estado.etapa - 1); }

  function reiniciar() {
    ["nome", "telefone", "observacao"].forEach((id) => { $(id).value = ""; });
    estado.servico = null;
    document.querySelectorAll(".opcao").forEach((o) => o.classList.remove("sel"));
    irParaEtapa(1);
  }

  function mascararTelefone(e) {
    let v = e.target.value.replace(/\D/g, "").slice(0, 11);
    if (v.length > 6) v = v.replace(/^(\d{2})(\d{4,5})(\d{4}).*/, "($1) $2-$3");
    else if (v.length > 2) v = v.replace(/^(\d{2})(\d+)/, "($1) $2");
    e.target.value = v;
  }

  function iniciar(servicos) {
    estado.servicos = servicos;
    const caixa = $("opcoes-servico");
    servicos.forEach((s, i) => {
      const b = document.createElement("button");
      b.type = "button"; b.className = "opcao"; b.dataset.id = s.id;
      b.style.setProperty("--cor", CORES[i % CORES.length]);
      b.innerHTML = '<span class="amostra"></span><span><strong></strong><small></small></span>';
      b.querySelector("strong").textContent = s.nome;
      b.querySelector("small").textContent = `${moeda(s.preco)} · ${duracao(s.duracaoMinutos)}`;
      b.addEventListener("click", () => selecionarServico(s.id));
      caixa.appendChild(b);
    });
    $("btn-avancar").addEventListener("click", avancar);
    $("btn-voltar").addEventListener("click", voltar);
    $("btn-novo").addEventListener("click", reiniciar);
    $("telefone").addEventListener("input", mascararTelefone);
    irParaEtapa(1);
  }

  return { iniciar, selecionarServico, duracao, moeda };
})();

const CORES = ["#b3315b", "#e9b7c6", "#3a2238", "#c98a9f", "#7d6275", "#d9a5b3"];
