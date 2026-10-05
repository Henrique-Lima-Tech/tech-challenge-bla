Preciso de ajuda com uma tarefa:

Vou descrever uma API que quero construir com o Claude Code e preciso que você transforme essa
descrição em um meta-prompt em inglês, bem organizado, que o agente vai seguir do planejamento até a
validação final. A API é de gerenciamento de tarefas (task management), com cadastro e autenticação
de usuários e o CRUD das tarefas de cada usuário. Vou usar Java 25 com Spring Boot 4.1.1 e Maven com
wrapper, partindo do projeto que já gerei pelo Spring Initializr na pasta task-management, com grupo
com.challenge.aitools, que hoje só tem o starter básico.

A arquitetura segue Clean Architecture com a regra de dependência (dependency rule) apontando para
dentro, separada em quatro módulos Maven sob um POM pai: domínio (domain), aplicação (application),
infraestrutura (infrastructure) e web. O projeto gerado pelo Initializr vira esse POM pai, e a
classe principal, o application.yaml e o teste de contexto vão para o módulo web, que é o único
executável. A regra de dependência fica garantida pelo próprio build, porque um import indevido em
domain ou application não compila. O módulo web depende de application e infrastructure,
infrastructure depende de application, e application depende de domain. Dentro de cada módulo, os
pacotes seguem com.challenge.aitools.taskmanagement.<camada>.<feature>, sem o underscore do pacote
gerado, com as features task, user e shared. No domínio ficam os modelos de tarefa (Task), status
(TaskStatus) e usuário (User), as invariantes (invariants) e as exceções de negócio (domain
exceptions), em Java puro, sem Spring, JPA, Jackson ou Bean Validation. Na aplicação ficam os casos
de uso (use cases), cada um com sua interface como porta de entrada (input port), os serviços
(services) que os implementam, também sem Spring ou JPA, e as portas de saída (output ports) para
repositório de tarefas (TaskRepository), repositório de usuários (UserRepository), hash de senha
(PasswordHasher), emissão de token (TokenIssuer) e relógio (Clock). Os casos de uso são registrar
usuário (RegisterUser), fazer login (Login), criar tarefa (CreateTask), listar as tarefas do usuário
(ListTasks), buscar tarefa (GetTask), atualizar tarefa (UpdateTask) e excluir tarefa (DeleteTask), e
os de tarefa sempre recebem o id do usuário autenticado.

O usuário é simples, com id, nome (name), email único (email) e hash da senha (passwordHash). O
registro (register) recebe nome, email e senha (password) de 8 a 72 caracteres, já que o BCrypt só
considera os primeiros 72 bytes. O login recebe email e senha e devolve um token de acesso
(accessToken) JWT HS256 válido por uma hora, assinado com uma chave vinda de variável de ambiente. O
email é comparado sem diferenciar maiúsculas e sem espaços nas pontas, e não haverá perfis (roles),
refresh token, logout nem recuperação de senha. A tarefa (Task) tem id, título (title), descrição
(description), status, data de vencimento (dueDate), data de criação (createdAt), data de última
atualização (updatedAt) e dono (owner). O título é obrigatório, sem espaços nas pontas, com 1 a 120
caracteres, e a descrição é opcional, com até 2000. O status é TODO, IN_PROGRESS ou DONE, começa
como TODO quando não informado e pode mudar livremente entre os três valores, sem regra de
transição. A data de vencimento é obrigatória, no formato ISO (2026-10-31), e não pode estar no
passado na criação. Na atualização, ela só é validada se mudar, então manter uma data já vencida é
permitido, e o dia de hoje vem de um Clock injetado para os testes controlarem o tempo. As datas de
criação e de atualização são definidas pelo servidor e são somente leitura, e o dono vem sempre do
token, nunca do corpo da requisição.

As rotas ficam sob /api/v1. O registro (POST /api/v1/auth/register) e o login (POST
/api/v1/auth/login) são públicos, e as rotas de tarefas exigem token. Criar (POST /api/v1/tasks)
responde 201 com o cabeçalho Location. Listar (GET /api/v1/tasks) é paginado, com page a partir de
0, size de 1 a 100 e padrão 20, filtro opcional por status e ordenação por dueDate crescente e
depois por id, sempre no formato de página (content, page, size, totalElements, totalPages). Buscar
(GET /api/v1/tasks/{id}) devolve a tarefa, atualizar (PUT /api/v1/tasks/{id}) substitui title,
description, status e dueDate, e excluir (DELETE /api/v1/tasks/{id}) responde 204. Tarefa de outro
usuário responde 404, e não 403, para não revelar que ela existe, e os demais casos de borda (edge
cases) também precisam de tratamento e teste. Um id não numérico ou menor que 1 responde 400, JSON
malformado ou corpo vazio responde 400, status desconhecido responde 400 indicando o campo, e id,
ownerId, createdAt ou updatedAt enviados no corpo respondem 400 em vez de serem ignorados. Uma
página além do fim responde 200 com conteúdo vazio e os totais reais, email já cadastrado responde
409, qualquer falha de login responde o mesmo 401 genérico, e token ausente, inválido ou expirado
responde 401. Os erros saem como ProblemDetail, conforme a RFC 9457, de um único
@RestControllerAdvice, com type, title, status, instance e detail fixo em inglês. Erros de validação
trazem detail "Validation failed" e uma lista errors com field e message, e nenhuma resposta expõe
stack trace, mensagem do banco ou valor rejeitado.

Para persistência uso H2 em arquivo na aplicação e em memória nos testes, Flyway para o schema e
para os dados de demonstração (seed data), com um usuário demo e algumas tarefas dele, e Hibernate
apenas validando o schema (ddl-auto validate). A segurança usa o resource server OAuth2 do Spring
Security para validar o JWT, sem filtro próprio, e BCrypt para as senhas. Na entrada uso Bean
Validation, com mensagens escritas explicitamente em inglês para não depender do idioma da máquina,
e no código uso Lombok, permitido em todos os módulos por ser só de compilação, MapStruct apenas em
infrastructure e web, e records para DTOs, comandos (commands) e resultados (results). No Spring
Boot 4, os starters são spring-boot-starter-webmvc, spring-boot-starter-data-jpa,
spring-boot-starter-flyway, spring-boot-starter-validation e
spring-boot-starter-security-oauth2-resource-server, e os de teste são separados por tecnologia,
como spring-boot-starter-webmvc-test e spring-boot-starter-data-jpa-test. Mocks de beans usam
@MockitoBean, o Jackson é o 3, o type do ProblemDetail precisa ser definido como about:blank pelo
handler, os processadores de anotação seguem a ordem Lombok, MapStruct e lombok-mapstruct-binding, e
o MapStruct precisa de versão fixa no POM. Qualquer outra API do Boot 4 deve ser confirmada
compilando, nunca suposta a partir do Boot 3.

A camada web valida o formato da entrada, o domínio protege as invariantes, os adaptadores
(adapters) traduzem exceções de framework em exceções do domínio, e cada exceção de negócio tem seu
handler com o status correto. Entidades JPA não saem de infrastructure, os serviços de application
são registrados como beans por classes de configuração no módulo web, a injeção é sempre pelo
construtor e todo valor não reatribuído é final, em campos, parâmetros, variáveis locais e catch. As
únicas exceções a essa regra são os campos injetados por framework nos testes, os componentes de
record e os parâmetros de lambda. Comentários só explicam o que o código não diz sozinho. Logs com
@Slf4j ficam na infraestrutura e na web, com DEBUG para o fluxo normal, INFO para fatos de
inicialização e um único WARN com contexto quando uma falha externa é tratada, sem registrar senha,
token, cabeçalho Authorization, email ou corpo de requisição. Tudo no projeto é em inglês, nenhum
segredo fica no código, vale sempre a solução mais simples que passa nos testes, sem abstrações para
o futuro, e nenhuma dependência além dessas entra sem consulta. Testes de arquitetura, ferramentas
de cobertura com limite mínimo, CI e documentação OpenAPI ficam fora do escopo.

Os testes seguem TDD no domínio e na aplicação, com o teste escrito primeiro, a falha mostrada, o
mínimo implementado e o teste passando, e nos adaptadores e na web eles podem vir junto do código.
Os nomes seguem should<Result>When<Condition>, os pacotes espelham os de produção e o corpo de cada
teste é dividido em // given, // when e // then, com // when & then quando ação e verificação são
uma única expressão, regra que não vale para métodos auxiliares. O domínio é testado com JUnit puro
e AssertJ, os casos de uso com Mockito nas portas, a persistência com @DataJpaTest, os controllers
com @WebMvcTest cobrindo status, JSON, validação e segurança, e alguns testes de integração com
@SpringBootTest percorrem registro, login e o CRUD completo. Cada regra e cada caso de borda tem
pelo menos um teste, toda classe de domínio e de aplicação tem seu teste correspondente, e nenhum
teste é apagado, desativado ou enfraquecido para passar.

O trabalho acontece em três fases com parada para aprovação. No planejamento (planning), o agente lê
o projeto e entrega o contrato da API (API contract), as regras numeradas (rules) ligadas a estes
requisitos, as decisões (decisions), as perguntas em aberto (open questions) e as etapas de
implementação (implementation tasks), pequenas e ordenadas de dentro para fora, e só segue depois da
minha aprovação. Na implementação (implementation), ele faz uma etapa por vez com TDD e, ao final de
cada uma, mostra os arquivos alterados, a saída dos testes e as mensagens de commit sugeridas, com
test: antes de feat:, fazendo commit apenas quando eu disser "commit" e perguntando antes de
qualquer coisa fora do plano. Na validação (validation), ele roda o ./mvnw verify completo, sobe a
aplicação e testa o fluxo com curl, incluindo casos de erro, revisa o código contra estes
requisitos, escreve o README com setup, credenciais de demonstração, rotas e testes, e entrega um
relatório de validação (validation report) honesto, com o que verificou em cada ponto, o que
corrigiu e como tratou casos de borda, autenticação e validações.

Organize tudo isso no meta-prompt usando os componentes de engenharia de prompt de forma
identificável. O papel (role) é o de um engenheiro Java sênior com experiência em Spring Boot, Clean
Architecture, TDD e APIs seguras. O contexto (context) traz o projeto e a stack. A instrução clara
(clear instruction) reúne todos os requisitos, regras e casos de borda. Os exemplos (few-shot
examples) são curtos e servem de referência, como um teste em given, when, then, um erro de
validação em ProblemDetail, uma regra do plano ligada ao requisito, a assinatura de um caso de uso
com sua porta, um par de mensagens de commit e um item do relatório de validação. As restrições
(constraints) dizem o que o agente não deve fazer, e o formato de saída (output format) define o que
ele entrega em cada fase. Use títulos e listas curtas, descreva a estrutura do projeto em texto, sem
árvores de pastas, diagramas ou esqueletos de código extensos, não omita nada do que descrevi e não
acrescente funcionalidades. Salve o meta-prompt em um arquivo Markdown em
task-management/docs/meta-prompt.md, contendo apenas o meta-prompt, pronto para ser entregue ao
agente. Na resposta, escreva em português uma lista curta com suposições e pontos que devo revisar,
apontando qualquer contradição ou lacuna em vez de decidir por conta própria.
