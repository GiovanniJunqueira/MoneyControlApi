# Financeiro API

Backend do sistema de controle financeiro, com dois módulos independentes:

- **Gastos**: lançamento de despesas por categoria personalizada, com período fiscal configurável (dia de fechamento do mês).
- **Devedores**: controle de pessoas que te devem, com histórico de dívidas, pagamentos parciais/totais e status (pendente/parcial/quitado), também com período fiscal próprio.

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

### Configuração de período fiscal (`/module-settings`)
- `GET /module-settings/{module}` — `module` = `gastos` ou `devedores`
- `PUT /module-settings/{module}` — `{ closingDay: 25 }`

### Categorias (`/categories`)
- `GET /categories`
- `POST /categories` — `{ name, color, icon }`
- `PUT /categories/{id}`
- `DELETE /categories/{id}`

### Gastos (`/expenses`)
- `GET /expenses?period=2026-08` (opcional; sem o parâmetro usa o período atual)
- `POST /expenses` — `{ categoryId, amount, description, date }`
- `PUT /expenses/{id}`
- `DELETE /expenses/{id}`

### Devedores (`/debtors`)
- `GET /debtors` — lista pessoas com total devido
- `GET /debtors/{id}` — detalhe com todas as dívidas
- `POST /debtors` — `{ name, notes }`
- `PUT /debtors/{id}`
- `DELETE /debtors/{id}`

### Dívidas (`/debts`)
- `POST /debts` — `{ debtorId, amount, reason, date }`
- `PUT /debts/{id}` — `{ amount, reason, date }`
- `DELETE /debts/{id}`
- `POST /debts/{id}/payments` — `{ amount, date? }` — registra pagamento (parcial ou total); status recalculado automaticamente

### Dashboard (`/dashboard`)
- `GET /dashboard/gastos?period=2026-08` — total, gasto médio, quebra por categoria (com %) e lista de lançamentos do período
- `GET /dashboard/devedores?period=2026-08` — total emprestado, recebido, pendente, e quebra por pessoa com todas as dívidas

## Sobre o período fiscal

Cada módulo tem seu próprio `closingDay` (dia do mês em que fecha, de 1 a 28). O período vai do dia seguinte ao fechamento de um mês até o dia de fechamento do mês seguinte — não é o mês de calendário. A lógica está isolada em `util/FiscalPeriodCalculator.java`.

## Próximos passos sugeridos

- Adicionar testes (JUnit + Testcontainers para o Postgres)
- Rate limiting nos endpoints de auth
- Monitoramento de erros (ex: Sentry)
