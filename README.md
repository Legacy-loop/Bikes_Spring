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

Exemplo de cadastro:

```json
{
  "codigo": "BIKE-001",
  "modelo": "Mountain bike",
  "status": "DISPONIVEL"
}
```

O recurso permite cadastrar, localizar e listar bicicletas. A alteração do status e as locações ainda não fazem parte das funcionalidades implementadas.

## Validação e erros

As requisições com campos inválidos retornam `422 Unprocessable Content`. Conflitos de unicidade, como e-mail, CPF ou código de bicicleta duplicados, retornam `409 Conflict`. Recursos não encontrados retornam `404 Not Found`; erros de senha retornam `400 Bad Request`.

Rotas protegidas retornam `401 Unauthorized` quando falta autenticação válida e `403 Forbidden` quando o perfil autenticado não tem permissão. Os erros da API incluem informações como rota, método, status e mensagem.

## Auditoria e configurações

Usuários, clientes e bicicletas registram data de criação, data da última modificação, usuário que criou e usuário que modificou. O fuso horário está configurado para `America/Sao_Paulo`, e o locale da aplicação para `pt_BR`.

## Testes

Os testes de integração cobrem cadastro e consulta de usuários, alteração de senha, cadastro e acesso a clientes, e cadastro, busca e controle de acesso a bicicletas. Eles usam a configuração de testes em `src/test/resources/application.properties`.
