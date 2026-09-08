# 🛒 Sistema PDV (Ponto de Venda) - Documentação para Desenvolvedores

## 📌 Visão Geral do Projeto
Este projeto consiste em um **PDV (Ponto de Venda)** modular, robusto e production-ready desenvolvido em **Java com Spring Boot**.

A arquitetura foi estruturada como um **Monolito de Camada Única (Spring MVC + Thymeleaf)** para facilitar a manutenção, o deploy e a integração com interfaces web/mobile e gateways/APIs de parceiros (ex: iFood, 99Food).

---

## 🏗️ Decisões Arquiteturais & Padrões

* **Precisão Financeira:** Uso estrito de `BigDecimal` para evitar imprecisões de arredondamento de ponto flutuante em preços, subtotais e totais acumulados.
* **Consistência Temporal:** Manipulação de datas e horários via `OffsetDateTime` para garantir precisão com fuso horário (Time-Zone accuracy).
* **Persistência & Mapeamento Relacional:** JPA / Hibernate integrados ao Spring Data JPA.
* **Modelo Não-Destrutivo (Soft Delete):** Implementado no catálogo de produtos para preservar integridade histórica de vendas passadas.
* **Estado de Pedido:** Controle do ciclo de vida da venda gerido por ENUM (`statusPedido`).

---

## 📐 Estrutura do Modelo de Dados (Domain Entities)

| Entidade | Responsabilidade Principais | Atributos/Mapeamento Chave |
| :--- | :--- | :--- |
| **`Cliente`** | Cadastro de clientes | Identificação, contato e histórico |
| **`Produtos`** | Catálogo de itens à venda | Nome, categoria, preço (`BigDecimal`), status de exclusão lógica |
| **`pedido`** | Cabeçalho e estado da transação | Data/Hora (`OffsetDateTime`), Status (`statusPedido`), Valor Total (`BigDecimal`) |
| **`itensPedido`** | Tabela associativa / Itens da venda | Relacionamentos `@ManyToOne` com `pedido` e `produtos`, Quantidade, Preço Unitário e Subtotal |

---

## ⚙️ Funcionalidades e Serviços Implementados (`Service Layer`)

### 1. `clienteService`
* Operações de CRUD para gestão e consulta de clientes.
* Regras de validação pré-persistência.

### 2. `produtoService`
* Cadastro e atualização de produtos.
* Filtros por categoria e listagem exclusiva de itens ativos.
* Lógica de deleção lógica (*soft-delete*).

### 3. `pedidoService`
* **`novoPedido()`**: Instancia um novo pedido, grava o timestamp de abertura (`OffsetDateTime.now()`) e define o status inicial como `PREPARANDO`.
* **`atualizarStatus()`**: Altera com segurança o ciclo de vida do pedido (ex: `PREPARANDO` -> `PRONTO` -> `FINALIZADO`).
* **`listarStatus()`**: Filtro rápido para integração com telas de cozinha / KDS (Kitchen Display System).
* **`procurarID()`**: Busca detalhada do pedido por identificador único.

### 4. `itensPedidoService`
* **`adicionarItem()`**: Associa um produto a um pedido, extrai a precificação atual do produto, calcula o subtotal multiplicando a quantidade pelo valor (`BigDecimal.multiply`), armazena observações customizadas do item e persiste a entrada.
* **`listarPorPedido()`**: Retorna a listagem completa de itens atrelados a uma venda específica.

---

## 🛠️ Tecnologias Utilizadas
* **Java 17+**
* **Spring Boot** (Spring Data JPA, Spring Web / MVC)
* **Lombok** (Redução de boilerplate)
* **Banco de Dados Relacional** (PostgreSQL / MySQL)

---

## 🚩 Próximos Passos no Roteiro de Desenvolvimento

1. **Recálculo do Total do Pedido:** Acoplar a soma automática do `valorTotal` na entidade `pedido` dentro de `itensPedidoService.adicionarItem` utilizando o acumulador `BigDecimal.add()`.
2. **Camada de Controllers Spring MVC:** Desenvolver as rotas para captura de requisições e integração direta com o mecanismo de renderização.
   3. **Views Dinâmicas (Thymeleaf):** Estruturar os templates HTML em `src/main/resources/templates` para o Caixa, Cardápio e Painel da Cozinha.