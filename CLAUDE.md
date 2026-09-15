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

## Módulo Bets (banca de apostas) — completamente separado das abas

Adicionado a pedido do usuário como uma segunda "aplicação" dentro do mesmo backend/frontend, sem nenhuma relação com `Tab`/Gastos/Devedores. Só aparece pra quem tem `User.betsEnabled = true` (toggle em `PUT /auth/settings`), mas as tabelas e endpoints existem pra todo mundo — a checagem de visibilidade é só no frontend (`BetsLayout` redireciona pra `/` se `betsEnabled` for `false`).

- **Modelo**: `bet_houses` (casas de aposta do usuário, com `position` pra ordem definida por drag-and-drop), `bet_months` (um "mês" de acompanhamento — só pode existir **um aberto** por usuário, garantido por índice único parcial `WHERE end_date IS NULL`), `bet_unit_value_changes` (log append-only do valor da unidade ao longo do tempo), `bet_daily_balances` (uma entrada de saldo por casa por dia, unique `(house_id, date)`) e `bet_month_starting_balances` (snapshot do saldo de cada casa no instante em que o mês começou — ver abaixo).
- **Unidade monetária com histórico**: a pessoa define um valor de unidade (ex: R$10 = 1un) ao iniciar o mês, e pode mudar em qualquer dia — o novo valor vale a partir daquele dia, sem alterar dias anteriores. Resolvido em `BetService.resolveUnitValue()` via `findTopByMonthIdAndDateLessThanEqualOrderByDateDescCreatedAtDesc`.
- **Continuidade de saldo entre meses**: não existe cópia/duplicação de dado nenhuma. O saldo "atual" de uma casa é sempre a entrada mais recente em `bet_daily_balances` pra ela, **independente do mês** (`currentBalance()` não filtra por `month_id`). Ao clicar "Iniciar novo mês", `BetMonth.startingBanca` é um snapshot (soma do saldo atual de todas as casas naquele instante) — é só esse número que ancora o lucro/prejuízo do mês novo.
- **Painel em funil (mês → dia → casa)**: `GET /bets/months` lista o resumo de cada mês (lucro em R$ e unidades, `open` se é o atual); `GET /bets/months/{id}/days` calcula, **em memória, dia a dia**, o saldo de cada casa por carry-forward (se a casa não foi atualizada num dia, mantém o saldo do dia anterior) e o resultado (`saldo hoje − saldo ontem`), sem nada disso ser armazenado — só `bet_daily_balances` existe fisicamente. Editar o saldo de um dia (`POST /bets/houses/{id}/balance` com `date` opcional) só é permitido dentro do **mês aberto**; meses encerrados são histórico read-only (decisão do usuário, evita que uma correção tardia deixe a `startingBanca` já travada do mês seguinte inconsistente).
- **`bet_month_starting_balances`**: guarda o saldo de CADA casa (não só o total) no instante exato em que um mês começa. É necessário porque `listMonthDays` precisa de um "saldo antes do dia 1" por casa pra calcular o resultado do primeiro dia — resolver isso com uma query por data (`WHERE date < month.startDate`) quebra sempre que dois meses começam no mesmo dia (ver "Cuidado com empates" abaixo), já que a data sozinha não distingue "antes deste mês" de "mais cedo hoje, no mês anterior". `startMonth()` grava o snapshot; `listMonthDays()` só lê dele (nunca mais faz a query por data).
- **`bet_months.ending_banca`**: gravada direto no mês que está sendo fechado (dentro de `startMonth()`, no mesmo instante em que `startingBanca` do mês novo é calculada — os dois valores são o mesmo número). Existe porque `listMonthsHistory()` antes INFERIA a banca final de um mês fechado olhando a `startingBanca` do próximo mês na lista — o que quebrava (voltava pra 0) assim que esse próximo mês era excluído (`DELETE /bets/months/{id}`, pedido explícito do usuário pra apagar mês criado por engano). Meses fechados antes dessa coluna existir caem no fallback antigo (olhar o próximo mês) se `ending_banca` for `NULL`.
- **Saldo inicial do dia editável (`bet_daily_balances.opening_balance`)**: por padrão o "início do dia" de uma casa é sempre o saldo final do dia anterior (carry-forward, `opening_balance = NULL`). Mas a pessoa pode sobrescrever esse valor manualmente — serve pra registrar um depósito/saque na casa (ex: tirar dinheiro da banca) sem que a diferença conte como resultado de aposta: se ela ajusta o início pra bater com o saldo real pós-movimentação, `resultado = final − início ajustado` fica em 0 (ou só o resultado real da aposta, se também apostou naquele dia), em vez de aparecer como um prejuízo/lucro que nunca aconteceu. `listMonthDays()` usa `opening_balance` quando presente; senão usa o carry-forward normal — e o saldo final (`balance`) sempre continua sendo carregado pro dia seguinte, ajustado ou não, então a continuidade entre dias não quebra.
- **`DELETE /bets/months/{id}`**: exclusão simples, sem lógica especial — o cascade do banco (`ON DELETE CASCADE` em `bet_daily_balances`, `bet_unit_value_changes` e `bet_month_starting_balances`, todas com FK pra `month_id`) já limpa tudo que pertencia àquele mês. **Limitação conhecida**: apagar um mês do MEIO do histórico (não o mais antigo nem o mais recente) ainda pode deixar o mês anterior a ele com um `profitLoss` levemente impreciso, porque a mudança de banca que aconteceu DURANTE o mês excluído se perde — isso só afeta meses excluídos que tinham dado real (o caso comum, de apagar um mês vazio criado por engano, não sofre disso).
- **`BetService`** concentra toda a lógica (casas + mês + saldos) num service só, diferente do Financeiro que separa em vários — o domínio é pequeno o suficiente pra não precisar dividir.

### Cuidado com empates em `ORDER BY` de datas

Esse módulo já pegou o mesmo bug duas vezes: qualquer `ORDER BY` numa coluna de data (`date`, `start_date`) sem desempate quebra quando dois registros caem no mesmo dia — e isso acontece na prática (corrigir um valor errado e reiniciar o mês/trocar a unidade de novo no mesmo dia é um fluxo real, não só de teste). Por isso:
- `bet_unit_value_changes` e `bet_months` têm uma coluna `created_at` (timestamp, não date) só pra desempate — toda query de "o mais recente" usa `OrderBy..DescCreatedAtDesc`, nunca só `OrderByDateDesc`/`OrderByStartDateDesc`.
- Se adicionar uma nova tabela/query nesse módulo que dependa de "qual registro é mais recente" baseado numa coluna `DATE` (não `TIMESTAMP`), adiciona `created_at` nela também desde o início.

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
