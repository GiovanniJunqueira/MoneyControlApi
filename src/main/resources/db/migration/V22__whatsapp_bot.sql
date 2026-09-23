-- Bot do WhatsApp pra cadastrar gastos por mensagem. whatsapp_phone liga o numero a uma conta
-- (cadastrado pela pessoa em Configuracoes, nao por um fluxo de verificacao - risco baixo pra um
-- grupo pequeno de confianca). whatsapp_pending_expenses guarda o gasto "no meio do caminho"
-- enquanto espera a pessoa escolher a categoria por WhatsApp.
ALTER TABLE users ADD COLUMN whatsapp_phone VARCHAR(20);
CREATE UNIQUE INDEX idx_users_whatsapp_phone ON users (whatsapp_phone) WHERE whatsapp_phone IS NOT NULL;

CREATE TABLE whatsapp_pending_expenses (
    id          UUID PRIMARY KEY,
    phone       VARCHAR(20) NOT NULL UNIQUE,
    tab_id      UUID NOT NULL REFERENCES tabs (id) ON DELETE CASCADE,
    amount      NUMERIC(12, 2) NOT NULL,
    description VARCHAR(255),
    date        DATE NOT NULL,
    created_at  TIMESTAMP NOT NULL
);
