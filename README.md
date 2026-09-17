# Financeiro API

Backend do sistema de controle financeiro. O usuário organiza os dados em **abas** (ex: uma por banco/conta) — cada aba é uma cópia independente do sistema, com dois módulos:

- **Gastos**: lançamento de despesas por categoria personalizada, com período fiscal configurável (dia de fechamento do mês) — próprio de cada aba.
- **Devedores**: controle de pessoas que te devem, com histórico de dívidas, pagamentos parciais/totais e status (pendente/parcial/quitado), também com período fiscal próprio da aba.

Um endpoint separado (`/dashboard/visao-geral`) soma os dados de todas as abas do usuário.

## Stack

- Java 17
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- PostgreSQL
- JWT (autenticação stateless)
- Maven

## Como rodar localmente

### 1. Banco de dados

Suba um PostgreSQL local (ou via Docker):

```bash
docker run --name financeiro-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=financeiro -p 5432:5432 -d postgres:16
```

### 2. Configurar variáveis

Toda config sensível vem de variáveis de ambiente, com defaults pensados pro Postgres local do passo 1 — ou seja, **não precisa configurar nada pra rodar localmente**. Variáveis disponíveis (todas opcionais em dev):

| Variável | Default local | Uso |
|---|---|---|
| `PORT` | `8080` | porta do servidor |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/financeiro` | URL JDBC do Postgres |
| `DATABASE_USERNAME` | `postgres` | usuário do banco |
| `DATABASE_PASSWORD` | `postgres` | senha do banco |
| `JWT_SECRET` | valor fraco de dev | segredo do JWT — **trocar por um valor forte em produção** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | origens permitidas, separadas por vírgula |
| `FRONTEND_URL` | `http://localhost:5173` | usada pra montar o link de "esqueci minha senha" enviado por e-mail |
| `MAIL_USERNAME` | vazio | conta Gmail usada pra enviar e-mail (login SMTP) — sem ela, o reset de senha não consegue enviar o e-mail |
| `MAIL_PASSWORD` | vazio | senha de app do Gmail (não a senha normal da conta) dessa mesma conta |

Em produção, defina essas variáveis no ambiente da plataforma de deploy (nunca commitando valores reais).

### 3. Rodar a aplicação

```bash
mvn spring-boot:run
```

> Dica: se preferir usar o Maven Wrapper (não incluso neste pacote), gere-o com `mvn -N wrapper:wrapper` e depois use `./mvnw spring-boot:run`.

A API sobe em `http://localhost:8080`. O schema é criado e versionado via **Flyway** (`src/main/resources/db/migration`) — não depende mais de `ddl-auto: update`. Também existe um `Dockerfile` pra build/execução em container:

```bash
docker build -t financeiro-api .
docker run -p 8080:8080 --env-file .env financeiro-api
```

## Endpoints principais

### Autenticação (`/auth`) — públicos
- `POST /auth/register` — `{ name, email, password }`
- `POST /auth/login` — `{ email, password }` → retorna `{ user, token }`
- `GET /auth/me` — dados do usuário logado

Para as demais rotas, envie o header: `Authorization: Bearer <token>`

### Abas (`/tabs`)
- `GET /tabs` — lista as abas do usuário
- `POST /tabs` — `{ name, color }`
- `PUT /tabs/{id}` — renomear/recolorir
- `DELETE /tabs/{id}` — apaga a aba e tudo dentro dela (categorias, gastos, devedores, dívidas — cascade no banco); bloqueado se for a última aba do usuário

Toda conta nova já nasce com uma aba "Geral". Todos os endpoints abaixo são aninhados sob `/tabs/{tabId}/...` — o `{tabId}` precisa ser de uma aba do próprio usuário.

### Configuração de período fiscal (`/tabs/{tabId}/module-settings`)
- `GET /tabs/{tabId}/module-settings/{module}` — `module` = `gastos` ou `devedores`
- `PUT /tabs/{tabId}/module-settings/{module}` — `{ closingDay: 25 }`

### Categorias (`/tabs/{tabId}/categories`)
- `GET /tabs/{tabId}/categories`
- `POST /tabs/{tabId}/categories` — `{ name, color, icon }`
- `PUT /tabs/{tabId}/categories/{id}`
- `DELETE /tabs/{tabId}/categories/{id}`

### Gastos (`/tabs/{tabId}/expenses`)
- `GET /tabs/{tabId}/expenses?period=2026-08` (opcional; sem o parâmetro usa o período atual da aba)
- `POST /tabs/{tabId}/expenses` — `{ categoryId, amount, description, date }`
- `PUT /tabs/{tabId}/expenses/{id}`
- `DELETE /tabs/{tabId}/expenses/{id}`

### Devedores (`/tabs/{tabId}/debtors`)
- `GET /tabs/{tabId}/debtors` — lista pessoas com total devido
- `GET /tabs/{tabId}/debtors/{id}` — detalhe com todas as dívidas
- `POST /tabs/{tabId}/debtors` — `{ name, notes }`
- `PUT /tabs/{tabId}/debtors/{id}`
- `DELETE /tabs/{tabId}/debtors/{id}`

### Dívidas (`/tabs/{tabId}/debts`)
- `POST /tabs/{tabId}/debts` — `{ debtorId, amount, reason, date }`
- `PUT /tabs/{tabId}/debts/{id}` — `{ amount, reason, date }`
- `DELETE /tabs/{tabId}/debts/{id}`
- `POST /tabs/{tabId}/debts/{id}/payments` — `{ amount, date? }` — registra pagamento (parcial ou total); status recalculado automaticamente

### Dashboard
- `GET /tabs/{tabId}/dashboard/gastos?period=2026-08` — total, gasto médio, quebra por categoria (com %) e lista de lançamentos do período, só dessa aba
- `GET /tabs/{tabId}/dashboard/devedores?period=2026-08` — total emprestado, recebido, pendente, e quebra por pessoa com todas as dívidas, só dessa aba
- `GET /dashboard/visao-geral?period=2026-08` — soma de **todas as abas** do usuário: total geral, quebra por categoria e por pessoa (mesmo nome em abas diferentes soma numa linha só), e o total de cada aba individualmente (usado pro gráfico de rosca da tela inicial)

## Sobre o período fiscal

Cada módulo tem seu próprio `closingDay` (dia do mês em que fecha, de 1 a 28). O período vai do dia seguinte ao fechamento de um mês até o dia de fechamento do mês seguinte — não é o mês de calendário. A lógica está isolada em `util/FiscalPeriodCalculator.java`.

## Próximos passos sugeridos

- Adicionar testes (JUnit + Testcontainers para o Postgres)
- Rate limiting nos endpoints de auth
- Monitoramento de erros (ex: Sentry)
