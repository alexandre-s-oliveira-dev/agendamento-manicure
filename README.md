# Sistema de Agendamento para Manicure e Pedicure

Sistema web para uma profissional de manicure e pedicure receber agendamentos online.
A cliente escolhe o serviço, a data e um horário realmente disponível, sem precisar criar conta.
A profissional tem um painel protegido por login para gerenciar agendamentos, serviços,
horários de atendimento e bloqueios.

## Funcionalidades

**Cliente (sem login)**
- Ver serviços, preços e duração
- Escolher serviço, data e horário (só aparecem horários realmente livres)
- Informar nome, telefone/WhatsApp e observação e confirmar o agendamento

**Profissional (com login)**
- Resumo do dia (agendamentos, clientes, serviços, tempo ocupado e livre) e próximos agendamentos
- Calendário por dia, semana e mês
- Confirmar, concluir, remarcar, cancelar e excluir agendamentos
- Criar, editar, ativar/desativar e excluir serviços
- Configurar o horário de funcionamento de cada dia (com intervalos)
- Bloquear um horário, um período ou um dia inteiro

**Regras do sistema**
- Dois agendamentos nunca ocupam o mesmo horário (conferido na API e também por uma regra no PostgreSQL)
- Considera horário de funcionamento, duração do serviço, agendamentos existentes, bloqueios e horários que já passaram

## Tecnologias

| Parte | Tecnologias |
|---|---|
| Frontend | HTML5, CSS3, JavaScript puro (sem frameworks) |
| Backend | Java 17+, Spring Boot 4.1, Spring Web, Spring Data JPA, Spring Security (login com JWT), Maven |
| Banco de dados | PostgreSQL |
| Hospedagem (planejada) | Netlify (frontend), Render (backend e banco) |

## Estrutura das pastas

```
agendamento-manicure/
├── README.md
├── .gitignore
├── frontend/
│   ├── index.html                  página da cliente
│   ├── admin/
│   │   ├── login.html              login da profissional
│   │   └── painel.html             painel da profissional
│   ├── css/                        style.css, admin.css
│   └── js/
│       ├── api.js                  único arquivo que fala com a API (URL da API fica aqui)
│       ├── app.js, agendamento.js, calendario.js      fluxo da cliente
│       └── login.js, painel*.js                       área da profissional
└── backend/
    ├── pom.xml
    ├── .env.example                modelo das variáveis de ambiente
    └── src/
        ├── main/java/com/belamao/agendamento/
        │   ├── controller/         recebe as requisições HTTP
        │   ├── service/            regras do sistema
        │   ├── repository/         acesso ao banco
        │   ├── entity/             tabelas
        │   ├── dto/                formato dos dados que entram e saem
        │   ├── exception/          erros claros para o frontend
        │   └── config/             segurança, CORS, dados iniciais
        ├── main/resources/application.properties
        └── test/                   testes automáticos
```

## Como instalar

Você precisa de:
- **JDK 17 ou mais novo**
- **Maven** (o IntelliJ IDEA já traz um embutido)
- **PostgreSQL** (versão 14 ou mais nova)
- Um jeito de servir o frontend: extensão **Live Server** do VS Code ou Node.js (`npx serve`)

## Como configurar o PostgreSQL

1. Instale o PostgreSQL e guarde a senha do usuário `postgres`.
2. Crie o banco (no pgAdmin ou no SQL Shell/psql):
   ```sql
   CREATE DATABASE agendamento;
   ```
3. As tabelas são criadas automaticamente na primeira execução do backend.

## Como configurar as variáveis de ambiente

Na pasta `backend`, copie `.env.example` para `.env` e preencha. **O arquivo `.env` nunca vai para o GitHub.**

| Variável | Para que serve | Obrigatória |
|---|---|---|
| `DB_PASSWORD` | Senha do PostgreSQL | Sim |
| `JWT_SECRET` | Texto aleatório com 32+ caracteres que assina o login | Sim |
| `ADMIN_NOME` / `ADMIN_EMAIL` / `ADMIN_SENHA` | Conta da profissional, criada na primeira execução (senha com 10+ caracteres) | Sim, na primeira vez |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` | Conexão com o banco (padrão: `localhost`, `5432`, `agendamento`, `postgres`) | Não |
| `CORS_ORIGENS` | Endereços do frontend autorizados, separados por vírgula | Não localmente |
| `JWT_EXPIRACAO_HORAS` | Validade do login (padrão: 8) | Não |
| `PORT` | Porta da API (padrão: 8080; a Render define sozinha) | Não |
| `DDL_AUTO` | Como o Hibernate cuida das tabelas (padrão: `update`) | Não |

Para gerar um `JWT_SECRET` no PowerShell:
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

## Como executar o backend

No IntelliJ: abra a pasta `backend` e rode `AgendamentoApplication`.

Ou pelo terminal, dentro de `backend/`:
```bash
mvn spring-boot:run
```

Teste: abra <http://localhost:8080/api/saude>. Deve responder `{"status":"ok","servico":"agendamento-api"}`.

Para rodar os testes automáticos: `mvn test` (o teste `AgendamentoApplicationTests` precisa do PostgreSQL ligado e do `.env`).

## Como executar o frontend

1. Confira em `frontend/js/api.js` o endereço da API (`BASE_URL`, padrão `http://localhost:8080/api`).
2. Sirva a pasta `frontend` por um servidor local (não abra o `index.html` com duplo clique):
   - **VS Code:** botão direito em `index.html`, **Open with Live Server** (porta 5500)
   - **Terminal:** `npx serve frontend` (porta 3000)
3. Site da cliente: `http://127.0.0.1:5500/`
4. Painel da profissional: `http://127.0.0.1:5500/admin/login.html`

Se usar outra porta, inclua o endereço em `CORS_ORIGENS`.

## Exemplos de endpoints

**Públicos (sem login)**

| Método | Endereço | O que faz |
|---|---|---|
| GET | `/api/saude` | Confere se a API está no ar |
| GET | `/api/servicos` | Lista os serviços ativos |
| GET | `/api/horarios-disponiveis?servicoId=1&data=2026-10-20` | Horários livres para o serviço na data |
| POST | `/api/agendamentos` | Cria um agendamento |
| POST | `/api/auth/login` | Login da profissional (devolve o token) |

Exemplo de agendamento:
```json
POST /api/agendamentos
{
  "servicoId": 1,
  "data": "2026-10-20",
  "horario": "10:00",
  "nomeCliente": "Maria",
  "telefone": "(98) 91234-5678",
  "observacao": ""
}
```

**Protegidos (exigem login: cabeçalho `Authorization: Bearer <token>`)**

| Método | Endereço | O que faz |
|---|---|---|
| GET | `/api/dashboard` | Números do resumo |
| GET | `/api/agendamentos?de=2026-10-01&ate=2026-10-31` | Agendamentos de um período (ou `?data=`) |
| PUT | `/api/agendamentos/{id}` | Confirmar, cancelar ou remarcar (`status`, `data`, `horario`) |
| DELETE | `/api/agendamentos/{id}` | Exclui um agendamento |
| POST / PUT / DELETE | `/api/servicos`, `/api/servicos/{id}` | Gerencia serviços |
| GET | `/api/horarios` | Lista os períodos de funcionamento |
| PUT | `/api/horarios/dia/{MONDAY...SUNDAY}` | Define todos os períodos de um dia (lista vazia = fechado) |
| POST / DELETE | `/api/bloqueios`, `/api/bloqueios/{id}` | Cria e remove bloqueios |

Erros seguem o formato `{"mensagem": "..."}` com o código HTTP adequado
(400 dados inválidos, 401 sem login, 404 não encontrado, 409 conflito de horário).

## Segurança

- Senhas guardadas com hash BCrypt, nunca em texto puro
- Rotas da profissional protegidas por login (Spring Security + JWT)
- Validação dos dados no frontend e no backend
- Nenhuma senha ou chave no código: tudo vem de variáveis de ambiente
- Bloqueio temporário do login: 5 erros seguidos no mesmo e-mail bloqueiam por 15 minutos (a contagem fica na memória do servidor e zera se ele reiniciar)
- HTTPS em produção (a Render e a Netlify já entregam HTTPS)

## Deploy (Netlify + Render)

Fluxo: **Frontend (Netlify) → API (Render) → PostgreSQL (Render)**.
O passo a passo detalhado será adicionado nesta seção na etapa de deploy.

## Próximas funcionalidades (arquitetura preparada)

Confirmação e lembrete pelo WhatsApp, cancelamento pela cliente, pagamento online/PIX, cupons,
avaliação da cliente, histórico de atendimentos, relatórios financeiros, múltiplas profissionais
e cadeiras, notificações.
