# Bikes API

API REST para gerenciamento de usuários, clientes e bicicletas, desenvolvida com Spring Boot. A API usa autenticação JWT, persiste os dados em MySQL e disponibiliza documentação interativa com Swagger.

## Requisitos

- Java 25
- MySQL em execução e um banco de dados chamado `bikes`

O acesso ao banco é configurado em `src/main/resources/application.properties`. Ajuste endereço, usuário e senha conforme seu ambiente; não compartilhe credenciais. O Hibernate atualiza o esquema automaticamente (`spring.jpa.hibernate.ddl-auto=update`).

A senha do banco é lida da variável de ambiente `DB_PASSWORD`. No PowerShell, defina-a antes de iniciar a aplicação:

```powershell
$env:DB_PASSWORD = "sua-senha-do-mysql"
$env:JWT_SECRET = "defina-um-segredo-aleatorio-com-pelo-menos-32-bytes"
```

Como a senha estava no histórico Git original, troque-a no MySQL antes de continuar usando essa conta.

## Executar

Na raiz do projeto, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Para executar os testes:

```powershell
.\mvnw.cmd test
```

A aplicação inicia, por padrão, em `http://localhost:8080`.

## Documentação da API

Com a aplicação em execução, acesse [Swagger UI](http://localhost:8080/docs-bikes.html). A documentação OpenAPI também está disponível em `http://localhost:8080/docs-bikes`.

No Swagger, use **Authorize** para informar o token JWT. As rotas de documentação são públicas; as rotas de negócio, exceto cadastro de usuário e autenticação, exigem token.

## Autenticação e perfis

O cadastro público cria usuários com perfil `CLIENTE` por padrão. O e-mail é usado como nome de usuário e deve ser único; a senha deve conter exatamente seis caracteres. As senhas são armazenadas com BCrypt.

Para autenticar, envie o e-mail e a senha. A resposta contém um token que deve ser enviado nas rotas protegidas no cabeçalho `Authorization`, com o prefixo `Bearer`.

```http
Authorization: Bearer <token>
```

O corpo de resposta é um JSON com o campo `token`:

```json
{
  "token": "<token retornado pela API>"
}
```

Os tokens expiram após 20 minutos. Não há sessão de servidor: cada requisição protegida deve enviar o token.

**Perfis disponíveis**

- `CLIENTE`: consulta e altera a própria conta, cadastra um cliente vinculado à sua conta e consulta os próprios dados de cliente.
- `ADMIN`: lista usuários e clientes, consulta usuários ou clientes por ID e gerencia bicicletas.

O endpoint público de cadastro não permite escolher perfil; novos usuários são `CLIENTE`. Não existe rota para criar administradores: a configuração inicial de um `ADMIN` deve ser feita por um meio administrativo confiável, usando o perfil `ROLE_ADMIN` e uma senha armazenada no formato BCrypt.

A aplicação exige `JWT_SECRET` com pelo menos 32 bytes para assinar e validar tokens. Guarde essa chave em local seguro e use a mesma em todas as instâncias da API.

## Endpoints

Todas as rotas usam o prefixo `/api/v1`.

### Autenticação

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/auth` | Público | Autentica e retorna um JWT |

Exemplo de requisição:

```json
{
  "username": "cliente@example.com",
  "password": "123456"
}
```

### Usuários

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/usuarios` | Público | Cadastra usuário com perfil `CLIENTE` |
| `GET` | `/usuarios/{id}` | ADMIN ou o próprio CLIENTE | Busca usuário por ID |
| `PATCH` | `/usuarios/{id}` | Usuário autenticado dono do ID | Altera a própria senha |
| `GET` | `/usuarios` | ADMIN | Lista usuários |

O corpo de `PATCH /usuarios/{id}` deve conter os três campos. Todos são obrigatórios e devem ter exatamente seis caracteres:

```json
{
  "senhaAtual": "123456",
  "novaSenha": "654321",
  "confirmaSenha": "654321"
}
```

Quando a senha atual está incorreta ou as novas senhas não coincidem, a API retorna `400`. Em caso de sucesso, retorna `204 No Content`, sem corpo.

### Clientes

Cada cliente está vinculado a exatamente um usuário, e cada usuário pode ter no máximo um cadastro de cliente. O CPF também é único e deve conter 11 dígitos. Nome, telefone e e-mail são obrigatórios; o e-mail deve ser válido.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/clientes` | CLIENTE | Cadastra o cliente vinculado ao usuário autenticado |
| `GET` | `/clientes/me` | CLIENTE | Consulta os próprios dados |
| `GET` | `/clientes/{id}` | ADMIN | Busca cliente por ID |
| `GET` | `/clientes` | ADMIN | Lista clientes |

Exemplo de cadastro:

```json
{
  "nome": "Maria da Silva",
  "cpf": "12345678901",
  "telefone": "47999999999",
  "email": "maria@example.com"
}
```

Um cliente não pode consultar o cadastro de outro cliente. Para consultar clientes por ID ou listar todos, é necessário perfil `ADMIN`.

### Bicicletas

Todas as operações de bicicleta exigem autenticação e perfil `ADMIN`. Cada bicicleta tem um ID gerado pelo banco e um código próprio, único e diferente do ID. Os status aceitos são `DISPONIVEL`, `ALUGADA` e `MANUTENCAO`.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/bicicletas` | ADMIN | Cadastra bicicleta |
| `GET` | `/bicicletas/codigo/{codigo}` | ADMIN | Busca bicicleta pelo código |
| `GET` | `/bicicletas` | ADMIN | Lista bicicletas |
| `PATCH` | `/bicicletas/codigo/{codigo}/status` | ADMIN | Altera o status (exceto bicicleta alugada ou status `ALUGADA`) |

Exemplo de cadastro:

```json
{
  "codigo": "BIKE-001",
  "modelo": "Mountain bike",
  "status": "DISPONIVEL"
}
```

Para manutenção, envie `PATCH /bicicletas/codigo/BIKE-001/status`:

```json
{
  "status": "MANUTENCAO"
}
```

Uma bicicleta em `ALUGADA` não pode ter o status alterado manualmente, e o status `ALUGADA` só é definido pela solicitação de locação.

### Locações e devoluções

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/locacoes` | CLIENTE | Solicita a primeira bicicleta disponível |
| `GET` | `/locacoes` | ADMIN | Lista todas as locações |
| `POST` | `/locacoes/{id}/devolucao` | ADMIN | Registra a devolução no balcão |

O cliente informa a data limite e os dados de cartão para a simulação. A bicicleta é escolhida automaticamente entre as que estão `DISPONIVEL`; não é possível escolher uma bicicleta específica. A solicitação altera a bicicleta para `ALUGADA` e cria a locação como `ATIVA`, na mesma transação. Não há locação simultânea ativa para a mesma bicicleta.

Exemplo de `POST /locacoes`:

```json
{
  "dataPrevistaDevolucao": "2027-01-15T18:00:00",
  "numeroCartao": "4111111111111111",
  "nomeTitular": "MARIA DA SILVA",
  "validade": "12/29",
  "cvv": "123"
}
```

Os dados do cartão são validados, mas o pagamento é simulado: número, validade e CVV não são persistidos, retornados nem registrados pela API.

Exemplo de resposta ao solicitar uma locação:

```json
{
  "id": 1,
  "nomeCliente": "Maria da Silva",
  "codigoBicicleta": "BIKE-001",
  "dataRetirada": "2026-11-24T08:00:00",
  "dataPrevistaDevolucao": "2027-01-15T18:00:00",
  "dataDevolucao": null,
  "duracaoMinutos": null,
  "valor": null,
  "status": "ATIVA"
}
```

Somente um ADMIN registra a devolução. O corpo usa `problema: false` quando a bicicleta não apresenta defeito ou `true` quando precisa de manutenção:

```json
{
  "problema": false
}
```

Ao devolver sem problema, a bicicleta volta para `DISPONIVEL`; com problema, passa a `MANUTENCAO`. A locação fica `FINALIZADA`, registra a duração real e recebe o valor calculado. A tarifa padrão configurada em `bikes.tarifa-hora` é R$ 10,00 por hora. Minutos são arredondados para cima em horas iniciadas e há cobrança mínima de uma hora; por isso, qualquer locação finalizada custa pelo menos a tarifa de uma hora.

### Relatório com JasperReports

O arquivo `src/main/resources/reports/locacoes.jrxml` é o template visual do relatório, equivalente à estrutura que seria montada no Jaspersoft Studio: título, emissão, cabeçalho, linhas de detalhe, total e paginação. Na inicialização, o JasperReports compila o `.jrxml` em um modelo reutilizável; a cada solicitação, a aplicação preenche esse modelo com as locações e exporta o resultado para PDF.

ADMIN pode acessar `GET /api/v1/relatorios/locacoes` autenticado, ou abrir essa rota no Swagger. A resposta é um PDF inline chamado `relatorio-locacoes.pdf`.

## Validação e erros

As requisições com campos inválidos retornam `422 Unprocessable Content`. Conflitos de unicidade, como e-mail, CPF ou código de bicicleta duplicados, retornam `409 Conflict`. Recursos não encontrados retornam `404 Not Found`; erros de senha retornam `400 Bad Request`.

Rotas protegidas retornam `401 Unauthorized` quando falta autenticação válida e `403 Forbidden` quando o perfil autenticado não tem permissão. Os erros da API incluem informações como rota, método, status e mensagem.

## Auditoria e configurações

Usuários, clientes e bicicletas registram data de criação, data da última modificação, usuário que criou e usuário que modificou. O fuso horário está configurado para `America/Sao_Paulo`, e o locale da aplicação para `pt_BR`.

## Testes

Os testes de integração usam `WebTestClient`, banco H2 e os scripts SQL em `src/test/resources`. Cobrem usuários, clientes, bicicletas, alteração de status, locações, regras de devolução e geração/autorização do relatório PDF.

## Roteiro de demonstração

1. Criar um usuário CLIENTE pela API e autenticar em `POST /api/v1/auth`.
2. Cadastrar os dados do cliente em `POST /api/v1/clientes`.
3. Autenticar como ADMIN e cadastrar uma bicicleta `DISPONIVEL` em `POST /api/v1/bicicletas`.
4. Voltar ao token CLIENTE e solicitar uma locação em `POST /api/v1/locacoes`; conferir que o sistema escolheu a bicicleta e ela ficou `ALUGADA`.
5. Com ADMIN, registrar a devolução em `POST /api/v1/locacoes/{id}/devolucao`; demonstrar tanto o retorno a `DISPONIVEL` quanto a opção de enviar para manutenção.
6. Gerar e abrir o PDF em `GET /api/v1/relatorios/locacoes`.
