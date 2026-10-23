/* Calendário mensal simples. Só permite escolher datas futuras (até "diasMaximos" à frente). */
class Calendario {
  constructor(container, { aoEscolher, diasMaximos = 60 }) {
    this.container = container;
    this.aoEscolher = aoEscolher;
    const hoje = new Date();
    this.hoje = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
    this.limite = new Date(this.hoje); this.limite.setDate(this.limite.getDate() + diasMaximos);
    this.mes = new Date(this.hoje.getFullYear(), this.hoje.getMonth(), 1);
    this.selecionada = null;
    this.render();
  }

  mudarMes(delta) {
    this.mes = new Date(this.mes.getFullYear(), this.mes.getMonth() + delta, 1);
    this.render();
  }

  render() {
    const ano = this.mes.getFullYear(), mes = this.mes.getMonth();
    const titulo = this.mes.toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
    const primeiro = new Date(ano, mes, 1).getDay();
    const total = new Date(ano, mes + 1, 0).getDate();
    const podeVoltar = this.mes > new Date(this.hoje.getFullYear(), this.hoje.getMonth(), 1);
    const podeAvancar = new Date(ano, mes + 1, 1) <= this.limite;

    this.container.innerHTML = "";
    const topo = document.createElement("div");
    topo.className = "cal-topo";
    topo.innerHTML = `<button type="button" class="cal-nav" aria-label="Mês anterior" ${podeVoltar ? "" : "disabled"}>‹</button>
      <strong>${titulo}</strong>
      <button type="button" class="cal-nav" aria-label="Próximo mês" ${podeAvancar ? "" : "disabled"}>›</button>`;
    const [ant, prox] = topo.querySelectorAll("button");
    ant.addEventListener("click", () => this.mudarMes(-1));
    prox.addEventListener("click", () => this.mudarMes(1));

    const grade = document.createElement("div");
    grade.className = "cal-grade";
    ["D", "S", "T", "Q", "Q", "S", "S"].forEach((s) => {
      const c = document.createElement("span"); c.className = "cal-sem"; c.textContent = s; grade.appendChild(c);
    });
    for (let i = 0; i < primeiro; i++) grade.appendChild(document.createElement("span"));

    for (let dia = 1; dia <= total; dia++) {
      const data = new Date(ano, mes, dia);
      const iso = API.paraISO(data);
      const b = document.createElement("button");
      b.type = "button";
      b.className = "cal-dia";
      b.textContent = dia;
      b.disabled = data < this.hoje || data > this.limite;
      if (+data === +this.hoje) b.classList.add("hoje");
      if (iso === this.selecionada) b.classList.add("sel");
      b.addEventListener("click", () => { this.selecionada = iso; this.render(); this.aoEscolher(iso); });
      grade.appendChild(b);
    }
    this.container.append(topo, grade);
  }
}
