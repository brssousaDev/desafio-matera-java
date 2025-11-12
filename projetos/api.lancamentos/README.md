# API de Lançamentos Bancários

Esta é uma API RESTful desenvolvida em Java e Spring Boot como solução para um desafio. O objetivo é fornecer endpoints para gerenciamento de transações bancárias (débito e crédito) e consulta de saldo, garantindo consistência e segurança em operações concorrentes.

## 🚀 Como Executar

### Pré-requisitos

* Java 17 
* Apache Maven

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone git@github.com:brssousaDev/desafio-java-matera.git
    cd api.lancamentos/
    ```

2.  **Compile o projeto com Maven:**
    (Isso também irá executar a geração de código do OpenAPI)
    ```bash
    mvn clean install
    ```

3.  **Execute a Aplicação:**

    * **Via IDE (IntelliJ / Eclipse):**
      Localize a classe principal `com.desafio.java.api.lancamentos.LancamentosBancariosApplication` e execute-a.

    * **Via linha de comando:**
      Após o `mvn install`, execute o arquivo `.jar` gerado:
        ```bash
        java -jar target/api.lancamentos-0.0.1-SNAPSHOT.jar 
        ```

4.  **Acesse a Aplicação:**
    A aplicação estará disponível em `localhost:8080`.

    * **Endpoints da API:** `http://localhost:8080/api/v1`
    * **Documentação (Swagger UI):** `http://localhost:8080/swagger-ui/index.html`
    * **Definição OpenAPI (JSON):** `http://localhost:8080/v3/api-docs`

---

## 🛠️ Especificações Técnicas

### Abordagem API-First

O projeto adota a metodologia **API-First**. O contrato da API foi definido em OpenAPI 3.0 (veja `api.json`).

Utilizamos o plugin `openapi-generator-maven-plugin` para gerar automaticamente:
* As interfaces `ApiDelegate` (ex: `ContasApiDelegate`).
* Os DTOs (Data Transfer Objects) do modelo (ex: `TransacaoRequest`, `SaldoResponse`).

Isso garante que a implementação (`ContasApiDelegateImpl`) esteja sempre sincronizada com a especificação da API.

---

### Funcionalidades (Endpoints)

A API expõe dois endpoints principais:

* **`GET /api/v1/contas/{numeroConta}/saldo`**
    * **Descrição:** Obtém o saldo atual de uma conta específica.
    * **Parâmetro:** `numeroConta` (string).
    * **Resposta (200 OK):** `SaldoResponse`.

* **`POST /api/v1/contas/{numeroConta}/transacoes`**
    * **Descrição:** Realiza um ou mais lançamentos (débito/crédito) em uma conta. A operação é atômica: ou todas as transações são processadas, ou nenhuma é (rollback).
    * **Parâmetro:** `numeroConta` (string).
    * **Corpo da Requisição:** Uma lista de `TransacaoRequest`.
    * **Resposta (200 OK):** `SaldoResponse` com o saldo atualizado.

---

### Principais Dependências

* **Spring Boot 3:** (Web, Data JPA, HATEOAS, Validation).
* **Hibernate:** Para ORM e persistência de dados.
* **Spring Retry:** Usado para tratar concorrência (detalhes abaixo).
* **H2 Database:** Banco de dados em memória para facilitar a execução e testes.
* **OpenAPI Generator:** Plugin Maven para a geração de código API-First.

---

### Controle de Concorrência

Um requisito chave do desafio é garantir a consistência dos dados em requisições concorrentes. A API implementa isso usando **Lock Otimista**:

1.  **`@Version`:** A entidade `Conta` possui um campo `version`. O Hibernate usa isso para detectar se outra transação modificou o registro desde que ele foi lido.
2.  **`@Retryable`:** Se o Lock Otimista falhar (lançando `ObjectOptimisticLockingFailureException`), o `ContaService` está configurado com `@Retryable` para tentar reprocessar a transação automaticamente (até 5 tentativas).
3.  **`409 CONFLICT`:** Se todas as tentativas falharem, a API retorna um erro `409 CONFLICT`.

---

### Banco de Dados (H2) e Dados Iniciais

O projeto utiliza um banco de dados **H2 em memória**.

Ao iniciar a aplicação, o arquivo `src/main/resources/data.sql` é executado, populando o banco com as seguintes contas para facilitar os testes:

| numero\_conta | saldo |
| :--- | :--- |
| `1001-0` | 0.00 |
| `1002-1` | 0.00 |
| `2001-5` | 1500.75 |
| `2002-6` | 350.20 |
| `2003-7` | 9800.00 |

---

### Tratamento de Erros (API)

A API utiliza o `RestApiExceptionHandler` para retornar códigos de status HTTP claros e mensagens de erro padronizadas:

| Código HTTP | Status | Quando Ocorre |
| :--- | :--- | :--- |
| **400** | `BAD REQUEST` | Requisição inválida (ex: valor de débito/crédito negativo ou zero). |
| **404** | `NOT FOUND` | Conta não encontrada no sistema. |
| **409** | `CONFLICT` | Falha de concorrência. Ocorreu um conflito ao tentar atualizar o saldo (Lock Otimista) e as 5 tentativas de `retry` falharam. |
| **422** | `UNPROCESSABLE_ENTITY` | Erro de regra de negócio. Ocorre especificamente ao tentar debitar um valor maior que o saldo disponível (Saldo Insuficiente). |

---

## 🧪 Testes

O projeto possui uma suíte de testes robusta para garantir a qualidade e a corretude das regras de negócio e da API.

### Testes de Unidade

* **`ContaTest.java`:** Valida as regras de negócio da entidade `Conta`:
    * Garante que o débito só ocorre com saldo suficiente.
    * Testa a exceção `SaldoInsuficienteException`.
    * Testa a exceção `IllegalArgumentException` para valores nulos ou negativos.
* **`ContaServiceTest.java`:** Testa a camada de serviço (`ContaService`) usando mocks (Mockito):
    * Valida o processamento de múltiplas transações (débito e crédito).
    * Garante que `ContaNotFoundException` é lançada.
    * Verifica a **atomicidade**: se uma transação falhar (ex: Saldo Insuficiente), nenhuma transação anterior da lista deve ser permanentemente salva.

### Testes de Integração

* **`ContaControllerIntegrationTest.java`:** Testa a API de ponta a ponta (`@SpringBootTest` com `MockMvc`):
    * Simula requisições `POST` e `GET` nos endpoints reais.
    * Valida os códigos de status HTTP para sucesso (200 OK) e erro (422 Unprocessable Entity, 404 Not Found).
    * Confirma que, após um erro 422 (Saldo Insuficiente), o saldo da conta não foi alterado (rollback).

### Teste de Concorrência

* **`ContaConcorrenciaTest.java`:** Este é o teste mais crítico do projeto, validando o requisito de `thread-safety`.
    * Ele usa `@SpringBootTest` para rodar a aplicação completa com o banco de dados.
    * **Simulação:** Dispara **20 threads** (`ExecutorService`) que tentam debitar `R$ 1,00` da *mesma conta* ao *mesmo tempo* (controlado por `CountDownLatch`).
    * **Validação:** O teste verifica se o mecanismo de Lock Otimista (`@Version`) e o `@Retryable` funcionam.
    * **Resultado:** O teste confirma que o saldo final da conta é exatamente o `SALDO_INICIAL` menos o número de transações que obtiveram sucesso (sem falha de lock). Isso prova que o sistema **não perde dados e não corrompe o saldo** sob alta concorrência.
