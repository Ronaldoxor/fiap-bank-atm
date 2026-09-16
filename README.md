# FIAP Bank - Emulador de Caixa Eletrônico (ATM)

![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/Maven-Multi--Módulo-blue?style=for-the-badge&logo=apache-maven)
![FlatLaf](https://img.shields.io/badge/UI-FlatLaf_Dark-darkgreen?style=for-the-badge)
![SQLite](https://img.shields.io/badge/Banco-SQLite_(JDBC)-003B57?style=for-the-badge&logo=sqlite)
![Architecture](https://img.shields.io/badge/Architecture-Domain--Driven_Design_(DDD)-purple?style=for-the-badge)

> **FIAP - Engenharia de Software (2026)**  
> **Checkpoint 4 (CP4)** — Aplicação de Emulação de Caixa Eletrônico (ATM) construída em **Java 21**, **Swing (FlatLaf)** e orientada aos princípios de **Domain-Driven Design (DDD)**.

---

## 👥 Integrantes do Grupo

| Nome completo | RM |
| :--- | :---: |
| Kauê de Almeida Pena | 564211 |
| Eduardo Delorenzo Moraes | 561749 |
| Ronaldo Aparecido Monteiro Almeida | 565017 |
| William Queiroz | 565032 |

---

## 📌 Visão Geral

O **FIAP Bank ATM** é um emulador interativo de Caixa Eletrônico de alta fidelidade visual e comportamental. Desenvolvido para simular a experiência real de operação de um terminal bancário autoatendimento, o sistema oferece desde a validação de segurança de cartões/PIN até a dispensação simulada de cédulas e impressão de extrato térmico em popup.

A aplicação foi projetada com arquitetura limpa em camadas baseada em **DDD (Domain-Driven Design)**, garantindo desacoplamento entre regras de negócio, persistência de dados e a camada de apresentação visual.

Cada camada é um módulo Maven separado, e os dados ficam gravados em um banco **SQLite** acessado via **JDBC**.

---

## 🎯 Principais Funcionalidades

- **🔐 Autenticação Segura & Gestão de PIN**:
  - Leitura e validação de conta bancária e senha numérica de 4 dígitos.
  - Bloqueio automático de segurança da conta após **3 tentativas incorretas consecutivas**.
  - Reset de tentativas falhas ao autenticar com sucesso.
- **💵 Saque Eletrônico (Withdrawal)**:
  - Valores rápidos pré-definidos via botões físicos laterais (R\$ 20, R\$ 50, R\$ 100, R\$ 200, R\$ 500) e opção de valor personalizado.
  - Validação estrita de saldo disponível (`InsufficientFundsException`).
  - Controle e validação de **limite diário de saque** (`DailyLimitExceededException`).
  - Animação visual no compartimento de notas (efeito luminoso verde e alerta de retirada de cédulas).
- **📥 Depósito em Dinheiro (Deposit)**:
  - Entrada de valores numéricos via teclado.
  - Validação de valor mínimo positivo.
  - Animação visual de processamento de depósitos e envelopes.
- **💸 Transferência entre Contas (Transfer)**:
  - Transferência em tempo real para qualquer outra conta existente no sistema.
  - Validação da existência da conta de destino e verificação de conta bloqueada.
  - Proibição de transferências para a própria conta de origem.
  - Lançamento automático de movimentação de saída (`TRANSFER_OUT`) e entrada (`TRANSFER_IN`).
- **📊 Consulta de Saldo e Limite Diário**:
  - Exibição de saldo em moeda nacional formatada (`R$ X.XXX,XX`).
  - Exibição em tempo real do limite diário de saque restante.
- **🧾 Impressão de Extrato Térmico Virtual**:
  - Simulação de impressora lateral com aviso luminoso.
  - Emissão de extrato em janela pop-up estilizada como papel térmico (fonte monospaced), contendo as últimas transações e botão de destacar comprovante.
- **⌨️ Dupla Forma de Interação (Teclado Físico + Botões Virtuais)**:
  - Interface com botões laterais (L1, L2, L3 e R1, R2, R3) e teclado numérico virtual.
  - Suporte completo ao **teclado do computador** via interceptação global de eventos (`0-9`, `Enter` para confirmar, `Backspace` / `Esc` para apagar ou cancelar).
- **💡 Indicadores de Periféricos & LED Animated States**:
  - LED indicador de leitor de cartão piscando no estado de boas-vindas.
  - Slots de dispensador de dinheiro e impressora com feedback de cor e estado.
- **💾 Dados Persistentes**:
  - Saldos, transações, tentativas de senha e bloqueios ficam gravados no banco SQLite e continuam lá quando o caixa é aberto de novo.

---

## 🏗️ Arquitetura do Sistema (DDD)

```
fiap-bank-atm/
├── pom.xml                                  # POM agregador (packaging pom): módulos e versões centralizadas
├── run.bat                                  # Atalho para executar no Windows
│
├── domain/                                  # Camada de Domínio (Regras de Negócio Puras)
│   ├── pom.xml                              # Sem nenhuma dependência
│   └── src/main/java/com/fiap/bank/atm/domain
│       ├── exception                        # Exceções de negócio customizadas
│       │   ├── AccountBlockedException.java
│       │   ├── DailyLimitExceededException.java
│       │   ├── InsufficientFundsException.java
│       │   └── InvalidPinException.java
│       ├── model                            # Entidades e Objetos de Valor (Value Objects)
│       │   ├── Account.java                 # Entidade Principal da Conta Bancária
│       │   ├── BaseEntity.java              # Classe base com ID (UUID) e datas de criação/atualização
│       │   ├── Money.java                   # Value Object imutável para operações monetárias (BigDecimal)
│       │   ├── Transaction.java             # Entidade de Registro de Transações
│       │   └── TransactionType.java         # Enum dos tipos de transação (Saque, Depósito, Transferências)
│       └── repository                       # Contratos de Repositório
│           ├── ATMRepository.java           # Repositório genérico <T extends BaseEntity> com Optional
│           └── AccountRepository.java       # Estende ATMRepository<Account> e busca pelo número da conta
│
├── infrastructure/                          # Camada de Infraestrutura (Persistência)
│   ├── pom.xml                              # domain + driver SQLite JDBC
│   └── src/main
│       ├── java/com/fiap/bank/atm/infrastructure
│       │   ├── database
│       │   │   ├── ConnectionFactory.java   # Singleton que abre e fecha as conexões com o banco
│       │   │   └── DatabaseInitializer.java # Cria as tabelas e faz a carga inicial
│       │   ├── exception
│       │   │   └── DatabaseException.java   # Erro de acesso ao banco (unchecked)
│       │   └── persistence
│       │       └── AccountRepositoryJdbcImpl.java # AccountRepository com JDBC (PreparedStatement e ResultSet)
│       └── resources/META-INF/services      # Registro da implementação para o ServiceLoader
│
├── application/                             # Camada de Aplicação (Casos de Uso & Orquestração)
│   ├── pom.xml                              # domain (optional) + infrastructure (runtime)
│   └── src/main/java/com/fiap/bank/atm/application
│       ├── dto
│       │   ├── AccountViewDTO.java          # Record com os dados da conta para a tela
│       │   └── TransactionViewDTO.java      # Record com os dados de cada transação do extrato
│       ├── exception
│       │   └── AtmOperationException.java   # Traduz as exceções do domínio para a tela (com o motivo)
│       ├── factory
│       │   └── AtmServiceFactory.java       # Monta o AtmService ligado ao repositório
│       └── service
│           └── AtmService.java              # Orquestra autenticação, transações e estado da sessão
│
└── presentation/                            # Camada de Apresentação (UI / Swing)
    ├── pom.xml                              # Somente application + FlatLaf
    └── src/main/java/com/fiap/bank/atm
        ├── AtmApplication.java              # Classe principal (main)
        └── presentation
            ├── AtmFrame.java                # Janela principal do ATM com FlatLaf Dark Theme
            ├── AtmFrame.form                # Arquivo de layout visual do Swing Form
            └── ScreenState.java             # Enum da Máquina de Estados da Tela
```

### Módulos e dependências

O `pom.xml` da raiz é o projeto agregador (`packaging pom`): não tem código, apenas reúne os módulos e centraliza as versões das bibliotecas.

```
presentation ──► application ──► domain
                      │
                      └─(runtime)─► infrastructure ──► domain
```

| Módulo | Depende de | Responsabilidade |
| :--- | :--- | :--- |
| `domain` | nada | Regras de negócio puras: entidades, value objects, exceções e contratos de repositório. |
| `infrastructure` | `domain`, `sqlite-jdbc` | Persistência no banco SQLite com JDBC. |
| `application` | `domain` (`optional`), `infrastructure` (`runtime`) | Casos de uso (`AtmService`), DTOs e a fábrica do serviço. |
| `presentation` | `application`, `flatlaf` | Telas em Java Swing e a classe principal. |

- **Isolamento em tempo de compilação:** a `presentation` enxerga única e exclusivamente a `application`. Qualquer `import` de `domain` ou `infrastructure` na tela quebra o build com `package ... does not exist`.
- **`optional` e `runtime`:** o `domain` não é repassado para quem depende da `application`, e a `infrastructure` só entra na execução, então nenhuma classe da `application` consegue importá-la.
- **Ligação do repositório:** o `AtmServiceFactory` monta o `AtmService` com a implementação do `AccountRepository` encontrada pelo `ServiceLoader` do Java, registrada em `infrastructure/src/main/resources/META-INF/services/com.fiap.bank.atm.domain.repository.AccountRepository`.

---

## ⚙️ Detalhamento dos Componentes de Domínio

### 1. `Money` (Value Object)
- Classe imutável responsável por manipular valores monetários garantindo precisão decimal com `BigDecimal` (escala 2).
- Evita erros de arredondamento de ponto flutuante (`double`).
- Formatação automática no padrão brasileiro `pt-BR` (ex: `R$ 5.000,00`).
- Métodos utilitários de comparação (`isGreaterThan`, `isLessThan`, `isGreaterThanOrEqual`) e aritmética (`plus`, `minus`).

### 2. `Account` (Aggregate Root / Entity)
- Contém o número da conta, PIN armazenado, saldo (`Money`), limite diário de saque, total sacado no dia, status de bloqueio, contador de falhas de autenticação e histórico de transações.
- Encapsula todas as regras de negócio de saque, depósito, transferência e autenticação.

### 3. `ATMRepository<T>` e `AccountRepository` (Contratos do Domínio)
- `ATMRepository<T extends BaseEntity>` define `buscarPorId`, `salvar`, `remover` e `buscarTodos`. Só aceita entidades que herdam de `BaseEntity`.
- `AccountRepository` estende `ATMRepository<Account>` e acrescenta `findByAccountNumber`.
- As buscas de uma conta (`buscarPorId` e `findByAccountNumber`) devolvem `Optional<T>`, e quem chama trata a ausência com `orElseThrow`, `map` ou `ifPresent`, sem `null`.

### 4. `AtmService` (Application Service)
- Atua como a fachada da camada de aplicação.
- Gerencia o estado da conta atualmente autenticada (`currentAccount`).
- Garante a execução transacional salvando alterações no `AccountRepository`.
- Recebe valores em `BigDecimal` e devolve apenas DTOs em records (`AccountViewDTO` e `List<TransactionViewDTO>`) e classes do Java (`BigDecimal`, `Boolean` e `Optional`), nunca entidades do domínio.
- Traduz as exceções do domínio para `AtmOperationException`, que informa o motivo da falha (`ACCOUNT_BLOCKED`, `INVALID_PIN`, `INSUFFICIENT_FUNDS`, `DAILY_LIMIT_EXCEEDED`, `INVALID_OPERATION`) para a tela escolher a mensagem.

### 5. `ConnectionFactory`, `DatabaseInitializer` e `AccountRepositoryJdbcImpl` (Infraestrutura JDBC)
- `ConnectionFactory`: Singleton que carrega o driver, abre as conexões com o arquivo `fiap-bank-atm.db` (com as chaves estrangeiras ligadas) e fecha essas conexões.
- `DatabaseInitializer`: cria as tabelas e faz a carga inicial das contas de teste a cada abertura do caixa. `CREATE TABLE IF NOT EXISTS` e `ON CONFLICT DO NOTHING` evitam duplicar dados.
- `AccountRepositoryJdbcImpl`: usa JDBC puro, sem ORM: somente `PreparedStatement` com parâmetros `?` preenchidos pelos métodos `set`, e o `ResultSet` percorrido com `next()` para montar a conta e todas as transações dela.
- Como o teclado do caixa só tem números, o `AccountRepositoryJdbcImpl` busca a conta digitada no formato do banco (`123456` é buscada como `12345-6`).
- Cada gravação roda numa transação JDBC (`setAutoCommit(false)`, `commit()` e `rollback()` em caso de erro).
- O total sacado no dia (limite diário) é calculado com Streams a partir das transações de saque do dia.

### 6. `ScreenState` & `AtmFrame` (Máquina de Estados de Tela)
A interface gráfica opera sobre uma **Máquina de Estados Finitos (FSM)** representada pelo enum `ScreenState`:

| Estado (`ScreenState`) | Descrição |
| :--- | :--- |
| `WELCOME` | Tela inicial aguardando a digitação do número da conta. LED do cartão pisca verde. |
| `ENTER_PIN` | Solicitação da senha de 4 dígitos (exibida com asteriscos `****`). |
| `MAIN_MENU` | Menu principal com opções operacionais associadas aos botões laterais. |
| `WITHDRAW_SELECT` | Seleção de valores pré-definidos de saque (R\$ 20 a R\$ 500) ou valor customizado. |
| `WITHDRAW_CUSTOM` | Campo para digitação de valor específico de saque. |
| `DEPOSIT_INPUT` | Campo para digitação de valor de depósito em dinheiro. |
| `TRANSFER_ACCOUNT` | Entrada do número da conta de destino para transferência. |
| `TRANSFER_VALUE` | Entrada do valor da transferência. |
| `SHOW_BALANCE` | Exibição do saldo disponível e limite diário restante. |
| `SHOW_STATEMENT` | Disparo da impressão do extrato. |
| `ANIMATION_*` | Estados temporários de animação (dispensador de cédulas, impressora e depósito). |
| `SUCCESS` / `ERROR` | Mensagens de confirmação de sucesso ou erros tratados do sistema. |

---

## 🗄️ Banco de Dados (SQLite)

O banco é o arquivo **`fiap-bank-atm.db`**, criado automaticamente na pasta de trabalho de onde o caixa é executado (com `mvn clean compile exec:java` na raiz, ele fica na raiz do projeto). Para voltar aos dados iniciais, basta fechar o caixa e apagar esse arquivo. Ele não vai para o GitHub (está no `.gitignore`).

| Tabela | Colunas | Conteúdo |
| :--- | :--- | :--- |
| `tb_account` | `id`, `agency`, `number`, `balance`, `status` (`ACTIVE` ou `BLOCKED`) | Contas bancárias |
| `tb_transaction` | `id`, `account_id` (FK), `type`, `amount`, `created_at` | Movimentações de cada conta |
| `tb_card` | `account_id` (PK e FK), `pin`, `daily_limit`, `failed_attempts` | Senha, limite diário de saque e tentativas de senha erradas de cada conta |

- Todas as instruções SQL usam `PreparedStatement`, com os valores passados pelos parâmetros `?` e pelos métodos `set`.
- O `type` da transação guarda o nome do enum (`WITHDRAWAL`, `DEPOSIT`, `TRANSFER_OUT`, `TRANSFER_IN`) e o `created_at` fica como texto `yyyy-MM-dd HH:mm:ss.SSS`.

---

## 🔑 Contas Pré-cadastradas para Teste (Seed Data)

Na primeira execução, a carga inicial cria as contas abaixo. O teclado do caixa só tem números, então a conta é digitada **sem o hífen**. As contas começam sem movimentações: o extrato vai sendo preenchido com o uso.

| Conta no banco | Digitar no caixa | PIN (Senha) | Saldo Inicial | Limite Diário Saque | Situação |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **`12345-6`** | `123456` | `1234` | **R\$ 1.500,00** | R\$ 1.500,00 | Ativa |
| **`98765-4`** | `987654` | `5678` | **R\$ 250,50** | R\$ 1.000,00 | Ativa |
| **`11111-1`** | `111111` | `9999` | **R\$ 0,00** | R\$ 500,00 | Bloqueada (3 tentativas erradas) |

---

## 🛠️ Tecnologias e Bibliotecas Utilizadas

- **Java 21**: Linguagem principal de programação (LTS).
- **Swing (Java GUI)**: Framework nativo de interface gráfica.
- **FlatLaf 3.5.1 (`com.formdev:flatlaf`)**: Look & Feel moderno e escuro para interfaces Swing.
- **JDBC (`java.sql`)**: API nativa do Java para acesso ao banco de dados (`Connection`, `PreparedStatement`, `ResultSet`).
- **SQLite JDBC 3.45.1.0 (`org.xerial:sqlite-jdbc`)**: Driver do banco SQLite, declarado somente no módulo `infrastructure`.
- **JUnit 5 (5.10.2)**: Framework de testes unitários (versão centralizada no POM raiz).
- **Apache Maven**: Gerenciamento de dependências, build multi-módulo e execução.

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **JDK 21** ou superior instalado e configurado nas variáveis de ambiente (`JAVA_HOME`).
- **Apache Maven 3.8+** instalado (ou via integração da IDE NetBeans / IntelliJ / Eclipse / VS Code).

---

### Opção 1: Linha de Comando (Terminal / Prompt)

1. Clone o repositório ou navegue até a pasta raiz do projeto:
   ```bash
   cd fiap-bank-atm
   ```

2. Compile todos os módulos, na ordem das dependências:
   ```bash
   mvn clean install
   ```

3. Compile e execute a aplicação via Maven:
   ```bash
   mvn clean compile exec:java
   ```
   *O `exec:java` é pulado nos outros módulos e roda só na `presentation`, onde fica a classe principal.*

---

### Opção 2: Executar via Script Batch (Windows)

No Windows, você pode executar diretamente o script configurado `run.bat`:
```cmd
run.bat
```
*O script localiza automaticamente o Maven do Apache NetBeans ou o `mvn` global e inicializa a aplicação.*

---

### Opção 3: Apache NetBeans / IntelliJ IDEA / Eclipse / VS Code

1. Abra a IDE e selecione **Open Project** apontando para a pasta raiz do projeto (onde se encontra o `pom.xml` agregador).
2. Aguarde a sincronização dos módulos e das dependências Maven (`flatlaf`, `sqlite-jdbc`).
3. Localize e execute a classe principal:  
   [AtmApplication.java](presentation/src/main/java/com/fiap/bank/atm/AtmApplication.java) (`com.fiap.bank.atm.AtmApplication`), no módulo `presentation`.

---

## 🧪 Rodando os Testes

Para executar a suíte de testes unitários com o Maven Surefire Plugin:
```bash
mvn test
```

### Conferindo a blindagem da camada de apresentação

1. Em qualquer classe do módulo `presentation`, adicione um import do domínio ou da infraestrutura, por exemplo:
   ```java
   import com.fiap.bank.atm.domain.model.Account;
   ```
2. Rode `mvn clean install`. O build falha com `package com.fiap.bank.atm.domain.model does not exist`, porque a `presentation` só enxerga a `application`.
3. Remova o import para voltar ao normal.

---

## 🎨 Destaques de Design e Usabilidade

- **Tema Escuro de Alta Performance (Slate & Neon)**: Tela em estilo monitor bancário CRT/LCD moderno com texto ciano/amarelo para facilitar a leitura.
- **Teclado Numérico & Teclas de Atalho**:
  - Tecla `1` a `0`: Digitação de valores e PIN.
  - Tecla `Confirmar` / `Enter`: Submete a ação atual.
  - Tecla `C` (Vermelha) / `Backspace` / `Esc`: Limpa a digitação ou cancela/volta de tela.
- **Janela de Extrato Destacável**:
  - Ao solicitar o extrato no menu principal, um diálogo em formato de **comprovante impresso térmico** é aberto ao lado do terminal com as movimentações recentes e saldo atualizado.

---

## 📝 Licença e Créditos

Desenvolvido para fins acadêmicos como parte do curso de **Engenharia de Software (2026)** da **FIAP**.  
Prof. Eduardo Ramos.
