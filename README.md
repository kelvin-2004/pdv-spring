# Sistema PDV (Ponto de Venda)

Sistema de gerenciamento e ponto de venda desenvolvido para controle de pedidos, produtos e clientes, ideal para estabelecimentos do ramo alimentício e comércio local.

## 🚀 Tecnologias Utilizadas

*   **Java 17**
*   **Spring Boot** (Web, Data JPA, Thymeleaf, DevTools)
*   **Hibernate / JPA**
*   **Banco de Dados H2** (Em memória)
*   **Lombok**
*   **Maven**

## 📋 Funcionalidades Principais

*   **Gestão de Produtos:** Cadastro de itens (marmitas, bebidas, sobremesas) com controle de preços, categorias e status de disponibilidade.
*   **Gestão de Clientes:** Cadastro de clientes com histórico de endereços, telefones e observações de entrega.
*   **Controle de Pedidos:** Registro de vendas associando clientes e itens do pedido.
*   **Carga Inicial Automática (`DataInitializer`):** O sistema inicializa o banco H2 com dados de teste cadastrados via código Java na primeira execução.

## ⚙️ Como Executar o Projeto

1. Certifique-se de ter o **Java 17** e o **Maven** instalados em sua máquina.
2. Clone o repositório ou abra a pasta do projeto no seu ambiente de desenvolvimento (como o **IntelliJ IDEA**).
3. Aguarde o Maven baixar as dependências do projeto.
4. Execute a classe principal **`PdvApplication.java`**.
5. Acesse o sistema pelo navegador através do endereço:
   * **Aplicação:** `http://localhost:8080`
   * **Console do H2 (Banco de Dados):** `http://localhost:8080/h2-console` *(URL de conexão: `jdbc:h2:mem:pdvdb`, Usuário: `sa`, Senha: em branco)*

## 📂 Configurações Importantes (`application.properties`)

O banco de dados roda inteiramente em memória e os dados iniciais são injetados de forma automatizada por componentes do Spring Boot, dispensando arquivos SQL externos.
