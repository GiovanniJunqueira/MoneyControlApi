# Contexto do projeto — Financeiro API

Backend do sistema de controle financeiro pessoal. O usuário organiza os dados em **abas** (`Tab`, ex: uma por banco) — cada aba tem seus próprios **Gastos** e **Devedores**, módulos independentes entre si. Este arquivo existe pra dar contexto rápido ao Claude Code sobre decisões já tomadas — evita re-perguntar coisas já definidas.

## Stack e por quê

- **Java 17 + Spring Boot 3** (Web, Data JPA, Security, Validation) — escolhido deliberadamente ao invés de Node.js porque é o que mais aparece em vagas do mercado (decisão do usuário).
- **PostgreSQL** via Spring Data JPA. Schema versionado com **Flyway** (`src/main/resources/db/migration`), `ddl-auto: validate` — não usa mais `update`.
- **JWT stateless** (biblioteca `jjwt`) para autenticação — multiusuário, cada um só vê seus próprios dados.
- **Maven**, sem Maven Wrapper incluso no pacote (gerar com `mvn -N wrapper:wrapper` se quiser). Existe um `Dockerfile` multi-stage pra build/execução em container, usado no deploy.
- **Deploy**: API no ar em https://financeiro-api-bc70.onrender.com (Render, free web service, via `render.yaml`), banco no **Neon** (Postgres free serverless), frontend planejado pro **Vercel**. Config sensível (DB, JWT secret, CORS, porta) é 100% via variáveis de ambiente — ver tabela no `README.md`.

## Decisões de produto (não óbvias pelo código)

1. **Multiusuário desde o início** — cada `User` tem suas próprias `Tab`s, e cada `Tab` tem seus próprios `Category`, `Expense`, `Debtor`, `Debt`, `ModuleSettings`. Toda query filtra por `tabId` (listagem) ou `userId` do JWT (ownership check em update/delete, via `CurrentUser.id()`) — ver seção "Abas" abaixo.
2. **Módulos Gastos e Devedores são totalmente independentes** — um gasto NÃO gera dívida automaticamente pra ninguém. Foi uma decisão explícita do usuário (perguntei e ele confirmou que quer os dois desacoplados).
3. **Cada módulo tem seu próprio "período fiscal"** (dia de fechamento do mês, configurável de 1 a 28, default 1) — independente por módulo **e por aba**. Duas abas podem ter dias de fechamento diferentes entre si, e dentro da mesma aba Gastos e Devedores também são independentes.
4. **Dívidas têm status com 3 estados**: `pendente` → `parcial` → `quitado`, recalculado automaticamente toda vez que um `DebtPayment` é registrado (soma pagamentos vs. valor total da dívida). Ver `DebtService.registerPayment()`.
5. **Categorias são 100% customizáveis pelo usuário** (nome, cor, ícone) — não existem categorias fixas/seed, mas são únicas por aba (duas abas podem ter cada uma sua própria categoria "Mercado").

## Abas (`Tab`) — multi-workspace

Adicionado depois do MVP inicial (o usuário queria separar os dados por banco/conta, ex: BTG vs Itaú, com uma visão consolidada). Pontos importantes:

- Toda conta nova ganha automaticamente uma aba **"Geral"** no registro (`AuthService.register()`). A migração `V2__add_tabs.sql` fez o mesmo retroativamente pra usuários que já existiam antes desse recurso — todos os dados antigos foram parar numa aba "Geral".
- `Category`/`Expense`/`Debtor`/`Debt`/`ModuleSettings` têm **os dois** campos `user` (redundante, mantido só pro ownership check em update/delete continuar simples) e `tab` (o campo que realmente importa pra listar/filtrar). Ver `findOwnedTab()` em cada Service.
- Excluir uma aba (`TabService.delete`) faz cascade no banco (`ON DELETE CASCADE` nas FKs de `tab_id`) — não tem lógica de limpeza manual. Bloqueado se for a última aba do usuário. A migration `V2` também corrigiu de passagem um bug preexistente: a FK `debt_payments.debt_id` não tinha cascade, então excluir uma dívida com pagamento registrado quebrava com violação de FK.
- `GET /dashboard/visao-geral` soma os dados de **todas** as abas do usuário — implementado como um loop em `DashboardService.visaoGeral()` que chama a mesma lógica por-aba (`gastosParaAba`/`devedoresParaAba`) pra cada `Tab` e funde os resultados. Categorias/pessoas com o mesmo **nome** em abas diferentes são somadas numa linha só (decisão do usuário).
- **Pegadinha de período na Visão Geral**: como cada aba pode ter um `closingDay` diferente, não dá pra deixar cada aba resolver seu próprio "período atual" de forma independente (duas abas podem discordar sobre qual é o mês corrente num dia de transição). A solução foi fixar uma única `period key` (formato `"YYYY-MM"`) ANTES do loop e passar a mesma pra `ExpenseService.resolvePeriod(key, closingDayDaAba)` de cada aba — cada uma calcula sua própria janela de datas a partir da mesma chave. Quando não vem `period` explícito na query, essa chave é calculada com `FiscalPeriodCalculator.getCurrentFiscalPeriod(1)` (closingDay=1 fixo como referência, não `YearMonth.now()` puro) — já teve um bug aqui onde usar o mês de calendário puro descolava do período que as telas por-aba mostravam por padrão.

## Onde entra o donut/gráfico de pizza do frontend

`GET /dashboard/visao-geral` retorna `abas: TabSummary[]` com `totalGastoPeriodo` de cada aba — é isso que dimensiona as fatias do gráfico de rosca na tela inicial do `financeiro-web` (`DonutTabChart.tsx`). Se mudar o formato desse campo, o frontend quebra o cálculo de proporção.

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

## Cuidado com `open-in-view: false` + associações lazy

`open-in-view` está desligado (`application.yml`) — de propósito, é a prática recomendada. Isso significa que a sessão do Hibernate fecha assim que o repository retorna, então **qualquer método de Service que acesse uma associação `@ManyToOne`/`@OneToMany` lazy depois da query precisa de `@Transactional(readOnly = true)`** (mantém a sessão aberta até o fim do método). Já apanhamos com isso: `DashboardService.gastos()`/`devedores()` (acessava `expense.getCategory().getName()` e `debt.getDebtor().getName()`), `DebtorService.list()`/`detail()` (acessava `debtor.getDebts()`) e `ExpenseService.list()` estavam sem a anotação e estouravam `LazyInitializationException` (500) assim que havia dado real — o dashboard vazio nunca pegava esse bug, só apareceu ao testar com um gasto/dívida de verdade. Se criar um novo método de leitura que atravesse uma associação lazy, adiciona `@Transactional(readOnly = true)` nele também.

## O que ainda falta (próximos passos conhecidos)

- [ ] Testes automatizados (JUnit + Testcontainers pro Postgres) — teriam pego o bug de `LazyInitializationException` acima antes de ir pra produção
- [ ] Rate limiting nos endpoints de `/auth` (app é multiusuário real, não só uso pessoal)
- [ ] Monitoramento de erros (ex: Sentry free tier)
- [x] Deploy efetivo no Render + banco no Neon — feito, API em https://financeiro-api-bc70.onrender.com
- [x] `CORS_ALLOWED_ORIGINS` atualizado no Render com a URL do Vercel (https://money-control-front-five.vercel.app)

## Frontend irmão

Existe um projeto frontend separado (`financeiro-web`, React + Vite + TS + Tailwind) que consome essa API. Ele espera exatamente os formatos de resposta definidos nos DTOs — se mudar um DTO aqui, o frontend provavelmente precisa de ajuste também. Ver `dto/expense/ExpenseResponse.java`, `dto/debt/DebtResponse.java` etc. como contrato.
