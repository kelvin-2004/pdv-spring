# Deploy em produção — Marmitas Sousa

Checklist do que falta para colocar o sistema no ar. Não contém segredos —
todos ficam no `.env.production` (gitignored).

## O que já está pronto

- Código compilando; perfil `prod` configurado em `application-prod.properties`.
- `.env.production` com credenciais Mercado Pago de produção (`APP_USR-`), segredo
  do webhook e chaves do Google/Geoapify.
- Webhook do Mercado Pago com validação de assinatura (`x-signature`).
- Checagens de segurança (`ProducaoSafetyChecks`) que impedem a app de subir em
  produção se faltar segredo do webhook ou se as chaves não forem `APP_USR-`.

## Passos restantes (na hora de comprar domínio + hospedagem)

1. **Domínio + DNS + HTTPS** — comprar o domínio, apontar para o servidor e ativar
   certificado TLS (https). Confirmar que o domínio final bate com o do `.env.production`
   (hoje `www.marmitasousa.com`).
2. **Banco de dados** — criar o MySQL de produção e preencher `DB_URL`, `DB_USER` e
   `DB_PASSWORD` no `.env.production`. No primeiro deploy rodar uma vez com
   `ddl-auto=update` para criar as tabelas; depois manter `validate`.
3. **Mercado Pago** — confirmar no painel que a URL do webhook de produção é
   `https://<dominio>/webhook/mercadopago` (com o caminho completo).
4. **Google OAuth** — em "Authorized redirect URIs" no Google Cloud Console, adicionar
   `https://<dominio>/site/google/callback`.
5. **Google Maps (recomendado)** — criar uma chave restrita ao domínio de produção
   em vez de reutilizar a de teste.
6. **Impressora** — definir `IMPRESSORA_TERMICA` com o nome exato da impressora do servidor.
7. **Senha admin** — trocar `PDV_LOGIN_PASSWORD` (uma já foi gerada no `.env.production`).
8. **Rodar** — subir com `SPRING_PROFILES_ACTIVE=prod` e o `.env.production` presente
   (o Spring o importa automaticamente).

## O que NÃO precisa de credencial nova

As credenciais do Mercado Pago (`APP_USR-`), o segredo do webhook, o client id/secret
do Google e as chaves de Maps/Geoapify **já estão prontos e não dependem do domínio**.
O que muda ao comprar o domínio são apenas duas URLs (webhook e redirect do Google),
não as credenciais em si.
