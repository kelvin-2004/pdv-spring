# Ponte de Impressão (Marmitas Sousa)

A VPS não tem impressora física. Este programa roda na máquina da loja (notebook/mini PC
sempre ligado, com a impressora térmica instalada) e busca as comandas pendentes no
servidor, imprime e marca como concluída.

## Pré-requisitos

- Java 17.
- Impressora térmica instalada nesta máquina.

## 1. Descobrir o nome exato da impressora

```bash
java PonteImpressao.java listar
```

## 2. Testar a impressão

```bash
IMPRESSORA=termica java PonteImpressao.java teste
```

## 3. Rodar a ponte

```bash
export PONTE_URL=https://marmitassousa.ddns.net
export PONTE_USUARIO=admin        # PDV_LOGIN_USERNAME de produção
export PONTE_SENHA='sua-senha'    # PDV_LOGIN_PASSWORD de produção
export IMPRESSORA=termica
export INTERVALO_SEGUNDOS=3
java PonteImpressao.java
```

A ponte fica em loop, buscando a cada `INTERVALO_SEGUNDOS`. Deixe rodando durante o
expediente (ou use `screen`/`tmux` para continuar após fechar o terminal).

## 4. Iniciar automaticamente com o notebook (systemd)

No notebook da loja há um serviço de usuário que já faz isso:

```bash
systemctl --user daemon-reload
systemctl --user enable --now ponte-impressao.service
loginctl enable-linger master   # continua rodando mesmo sem login
```

Para conferir se está rodando: `systemctl --user status ponte-impressao.service`.
