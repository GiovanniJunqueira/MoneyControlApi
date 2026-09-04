# Contexto do projeto — Financeiro API

Backend do sistema de controle financeiro pessoal, com dois módulos independentes: **Gastos** e **Devedores**. Este arquivo existe pra dar contexto rápido ao Claude Code sobre decisões já tomadas — evita re-perguntar coisas já definidas.

## Stack e por quê

- **Java 17 + Spring Boot 3** (Web, Data JPA, Security, Validation) — escolhido deliberadamente ao invés de Node.js porque é o que mais aparece em vagas do mercado (decisão do usuário).
- **PostgreSQL** via Spring Data JPA. Schema versionado com **Flyway** (`src/main/resources/db/migration`), `ddl-auto: validate` — não usa mais `update`.
- **JWT stateless** (biblioteca `jjwt`) para autenticação — multiusuário, cada um só vê seus próprios dados.
- **Maven**, sem Maven Wrapper incluso no pacote (gerar com `mvn -N wrapper:wrapper` se quiser). Existe um `Dockerfile` multi-stage pra build/execução em container, usado no deploy.
- **Deploy**: API no **Render** (free web service), banco no **Neon** (Postgres free serverless), frontend no **Vercel**. Config sensível (DB, JWT secret, CORS, porta) é 100% via variáveis de ambiente — ver tabela no `README.md`.

## Decisões de produto (não óbvias pelo código)

1. **Multiusuário desde o início** — cada `User` tem seus próprios `Category`, `Expense`, `Debtor`, `Debt`, `ModuleSettings`. Toda query filtra por `userId` do JWT (via `CurrentUser.id()`).
2. **Módulos Gastos e Devedores são totalmente independentes** — um gasto NÃO gera dívida automaticamente pra ninguém. Foi uma decisão explícita do usuário (perguntei e ele confirmou que quer os dois desacoplados).
3. **Cada módulo tem seu próprio "período fiscal"** (dia de fechamento do mês, configurável de 1 a 28, default 1) — também são independentes entre si. Um usuário pode fechar Gastos todo dia 25 e Devedores todo dia 5, por exemplo.
4. **Dívidas têm status com 3 estados**: `pendente` → `parcial` → `quitado`, recalculado automaticamente toda vez que um `DebtPayment` é registrado (soma pagamentos vs. valor total da dívida). Ver `DebtService.registerPayment()`.
5. **Categorias são 100% customizáveis pelo usuário** (nome, cor, ícone) — não existem categorias fixas/seed.

## A lógica mais importante do sistema: período fiscal

Está isolada em `util/FiscalPeriodCalculator.java`. Não é o mês de calendário — é uma janela custom que vai de `(closingDay + 1)` de um mês até `closingDay` do mês seguinte. Antes de mexer nisso, ler os comentários da classe com atenção; tem tratamento de meses com menos dias (clamp) que é fácil de quebrar.

## Estrutura de pacotes

```
com.financeiro.api/
  config/          # SecurityConfig (JWT, CORS)
  security/        # JwtService, JwtAuthenticationFilter, CurrentUser (helper pra pegar userId do contexto)
  entity/          # JPA entities
  repository/      # Spring Data JPA repositories
  dto/              # records de request/response, um subpacote por domínio
  service/         # lógica de negócio
  controller/      # REST endpoints
  util/            # FiscalPeriodCalculator
  exception/       # AppException + GlobalExceptionHandler (RestControllerAdvice)
```

Padrão seguido em todo módulo: `Controller` fino → `Service` com a lógica → `Repository`. DTOs são `record`s do Java, validados com Bean Validation (`jakarta.validation`).

## O que ainda falta (próximos passos conhecidos)

- [ ] Testes automatizados (JUnit + Testcontainers pro Postgres)
- [ ] Rate limiting nos endpoints de `/auth` (app é multiusuário real, não só uso pessoal)
- [ ] Monitoramento de erros (ex: Sentry free tier)
- [ ] Deploy efetivo no Render + banco no Neon (config já está pronta, falta provisionar)

## Frontend irmão

Existe um projeto frontend separado (`financeiro-web`, React + Vite + TS + Tailwind) que consome essa API. Ele espera exatamente os formatos de resposta definidos nos DTOs — se mudar um DTO aqui, o frontend provavelmente precisa de ajuste também. Ver `dto/expense/ExpenseResponse.java`, `dto/debt/DebtResponse.java` etc. como contrato.
