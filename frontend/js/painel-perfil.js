/* Aba "Meu perfil": a profissional troca nome, e-mail e senha. */
const PainelPerfil = (() => {
  const $ = (id) => document.getElementById(id);
  let aoErro = () => {};

  function mensagem(texto, deuCerto) {
    const m = $("perfil-msg");
    m.textContent = texto;
    m.style.color = deuCerto ? "#2e7d32" : "";
    m.hidden = false;
  }

  function limparSenhas() {
    $("perfil-atual").value = "";
    $("perfil-nova").value = "";
    $("perfil-repete").value = "";
  }

  async function carregar() {
    $("perfil-msg").hidden = true;
    limparSenhas();
    try {
      const dados = await API.requisitar("/perfil");
      $("perfil-nome").value = dados.nome;
      $("perfil-email").value = dados.email;
    } catch (e) {
      $("perfil-msg").style.color = "";
      aoErro(e, $("perfil-msg"));
    }
  }

  async function salvar() {
    const nome = $("perfil-nome").value.trim();
    const email = $("perfil-email").value.trim();
    const atual = $("perfil-atual").value;
    const nova = $("perfil-nova").value;
    const repete = $("perfil-repete").value;

    if (!nome) { mensagem("Digite o nome."); return; }
    if (!email || !$("perfil-email").checkValidity()) { mensagem("Digite um e-mail válido."); return; }
    if (nova && nova.length < 10) { mensagem("A nova senha precisa ter pelo menos 10 caracteres."); return; }
    if (nova !== repete) { mensagem("A repetição da nova senha não confere."); return; }

    const botao = $("btn-perfil-salvar");
    botao.disabled = true;
    try {
      const r = await API.requisitar("/perfil", {
        method: "PUT",
        body: JSON.stringify({ nome, email, senhaAtual: atual, novaSenha: nova }),
      });
      if (r.precisaEntrarNovamente) {
        mensagem("Salvo! Por segurança, entre de novo com os dados novos. Redirecionando...", true);
        setTimeout(() => { API.sair(); location.href = "login.html"; }, 2500);
        return;
      }
      sessionStorage.setItem("nome_admin", r.nome);
      $("saudacao").textContent = `Olá, ${r.nome}`;
      limparSenhas();
      mensagem("Dados salvos.", true);
    } catch (e) {
      $("perfil-msg").style.color = "";
      aoErro(e, $("perfil-msg"));
    } finally {
      botao.disabled = false;
    }
  }

  function iniciar(tratadorDeErro) {
    aoErro = tratadorDeErro;
    $("btn-perfil-salvar").addEventListener("click", salvar);
  }

  return { iniciar, carregar };
})();
