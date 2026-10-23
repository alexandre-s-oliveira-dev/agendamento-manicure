/* Página de login da profissional. */
(function () {
  if (API.sessao()) { location.href = "painel.html"; return; }

  const form = document.getElementById("form-login");
  const msg = document.getElementById("mensagem");
  const botao = document.getElementById("btn-entrar");

  // Botão "Mostrar / Ocultar" da senha
  const campoSenha = document.getElementById("senha");
  const verSenha = document.getElementById("ver-senha");
  verSenha.addEventListener("click", () => {
    const escondida = campoSenha.type === "password";
    campoSenha.type = escondida ? "text" : "password";
    verSenha.textContent = escondida ? "Ocultar" : "Mostrar";
  });

  // Faz a caixa de login "tremer" (só se a pessoa não pediu para reduzir movimento; o CSS cuida disso)
  function tremer() {
    const caixa = document.querySelector(".login-caixa");
    caixa.classList.remove("tremer");
    void caixa.offsetWidth; // reinicia a animação
    caixa.classList.add("tremer");
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    msg.hidden = true;
    const email = document.getElementById("email").value.trim();
    const senha = document.getElementById("senha").value;
    if (!email || !senha) {
      msg.textContent = "Informe o e-mail e a senha.";
      msg.hidden = false;
      tremer();
      return;
    }
    botao.disabled = true;
    botao.textContent = "Entrando…";
    botao.classList.add("carregando");
    try {
      const dados = await API.login(email, senha);
      API.salvarSessao(dados);
      location.href = "painel.html";
    } catch (erro) {
      msg.textContent = erro.message;
      msg.hidden = false;
      botao.disabled = false;
      botao.classList.remove("carregando");
      botao.textContent = "Entrar";
      tremer();
      campoSenha.focus();
      campoSenha.select();
    }
  });
})();
