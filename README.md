# kotlin-event-sourcing-app

Это стартовый шаблон для новых event-sourcing приложений на Котлин.

Стартовый шаблон приложения Event sourcing: https://github.com/stepin/kotlin-event-sourcing-app

В заметке "[Классический event sourcing](https://stepin.name/technoblog/048-event-sourcing-classic/)" разобраны основы, в "[Inline event sourcing](https://stepin.name/technoblog/049-inline-event-sourcing/)" разобрана архитектура этого шаблона.

Данный репозиторий представляет из себя пример приложения, а не отдельный движок. Пока что у меня нет уверенности, что этот движок можно использовать "как есть" в других приложениях. При этом есть уверенность, что начав с этого шаблона, вполне реально развивать приложения.

Данный проект является извлечением общей части из одного из моих личных проектов.  Это уже где-то 5ая версия движка (первая вообще была на Golang). При этом версия новая -- возможны какие-то шероховатости первое время.

Шаблон основывается на моем базовом шаблоне Котлин-приложений: https://github.com/stepin/kotlin-bootstrap-app

## События

Начинаем с выявления событий и сущностей.

Допустим, у нас есть простая бизнес-сущность Пользователь:

```kotlin
data class User(
  displayName: String,
  firstName: String,
  seconfName: String,
  email: String
)
```

И мы хотим поддержать следующие сценарии (события):
- регистрация пользователя
- смена имени
- удаление пользователя

Для простоты примера не будем обращать внимание на подтверждения и авторизацию.

Пример события регистрации пользователя:

```kotlin
data class UserRegistered(
  val email: String,
  val firstName: String?,
  val secondName: String?,
  val displayName: String,
  override val accountGuid: AccountGuid,
  override val aggregatorGuid: UserGuid = UUID.randomUUID(),
  override val guid: EventGuid = UUID.randomUUID(),
) : UserEvent(eventTypeVersion = 3)
```

- 4 основных поля: email, firstName, secondName, displayName
- guid самого событий (рандомный)
- aggregator guid = user guid  -- вот это неудобно, что нет синонима, но можно привыкнуть (и указан typealias UserGuid)
- account guid -- движок расчитан на мультиаккантовые приложения
- data class -- удобно. И еще удобнее, что UserEvent -- sealed класс, можно такие конструкции делать:

```kotlin
when (val e = event as UserEvent) {
  is UserMetaUpdated -> "updated $e"
  is UserRegistered -> "user registered with id $id ${meta.createdAt} $e"
  is UserRemoved -> "user ${e.email} deleted at ${meta.createdAt}"
}
```

Базовый класс для событий агрегата User выглядит так:

```kotlin
sealed class UserEvent(
  override val eventTypeVersion: Short = 0,
) : DomainEvent {
  override val aggregatorType: String
    get() = "user"

  override val eventType: String
    get() = this.javaClass.simpleName

  abstract override val aggregatorGuid: UserGuid
}
```

- реализуется интерфейс DomainEvent движка
- выставляется typealias UserGuid для aggregatorGuid -- необязательно, как документация
- выставляется тип агрегата
- выставляется тип события -- автоматически берется имя класса события (например, UserRegistered)
- выставляется версия события в 0 по умолчанию, но это значение событие может переопределить

По сути, от события движок требует 2 вещи:
- реализации интерфейса DomainEvent
- корректной сериализации и десериализации JSONB

Остальное на усмотрение разработчика. При этом базовый класс для всех событий агрегата считается хорошей практикой.

Про id/guid: в этом примере подразумевается, что команды работают с guid, а при необходимости join в SQL-запросах используется id (т.к. быстрее).

## Команда

У нас команда -- это либо отдельный Spring-сервис, либо метод внутри Spring-сервиса. По сути единственный критичный момент -- должен использоваться интерфейс `EventStorePublish` для публикации событий, а остальное движок не ограничивает.

Команда регистрации:

```kotlin
@Service
class RegisterUser(
  private val store: EventStorePublisher,
  private val userRepository: UserRepository,
) {

data class Params(
  val email: String,
  val firstName: String?,
  val secondName: String?,
  val displayName: String?,
)

sealed class Response {
  data class Created(val userGuid: UUID) : Response()
  data class Error(val errorCode: ErrorCode) : Response()
}

suspend fun execute(params: Params): Response = with(params) {
  val user = userRepository.findByEmail(email)
  if (user != null) {
    return Response.Error(ErrorCode.USER_ALREADY_REGISTERED)
  }

  val accountGuid = UUID.randomUUID()
  val userGuid = UUID.randomUUID()

  val userRegistered = UserRegistered(
    accountGuid = accountGuid,
    aggregatorGuid = userGuid,
    email = email,
    firstName = firstName,
    secondName = secondName,
    displayName = displayName ?: calcDisplayName(email, firstName, secondName),
  )
  store.publish(userRegistered)

  val accountCreated = AccountCreated(
    name = "Неизвестная компания",
    accountGuid = accountGuid,
    userGuid = userGuid,
  )
  store.publish(accountCreated)

  return Response.Created(userGuid)
}
}
```

Возвращаемые от команд значения зависят от бизнес-логики: могут ли быть бизнес-ошибки, нужно ли вернуть guid и т.п. В каких-то случаях может ничего не возвращаться.

## Проекторы

Пример 2-х проекторов в одном классе:

```kotlin
@Service
class UserProjector(
  private val userRepository: UserRepository,
  private val accountRepository: AccountRepository,
) {
  companion object : Logging

  @Projector
  suspend fun handleUserRegistered(e: UserRegistered, meta: EventMetadata) {
    val account = accountRepository.findByGuid(e.accountGuid)

    val u = UserEntity()
    u.accountGuid = e.accountGuid
    u.accountId = account?.id ?: 0
    u.guid = e.aggregatorGuid
    u.email = e.email
    u.displayName = e.displayName
    u.firstName = e.firstName
    u.secondName = e.secondName
    u.createdAt = meta.createdAt.toInstant(ZoneOffset.UTC)

    val savedUser = userRepository.save(u)
    logger.debug { "new user id: ${savedUser.id}" }
  }

  @Projector
  suspend fun handleUserRemoved(e: UserRemoved) {
    val user = getUser(e.aggregatorGuid)
    userRepository.delete(user)
  }

  private suspend fun getUser(userGuid: UUID) = userRepository.findByGuid(userGuid)
    ?: throw DomainException(ErrorCode.USER_NOT_FOUND)
}
```

- метод проектора должен быть в Spring-бине
- должна быть аннотация @Projector
- в классе может быть несколько методов -- ограничений нет
- первый аргумент -- событие
- второй (опционально) -- метаданные события
- метод должен быть suspend (в принципе, это ограничение можно снять, но сейчас так в движке и не планирую использовать не suspend-методы)
- исключение в проекторе отменит сохранение события

## Реакторы

```kotlin
@Service
class UserRegisteredEmailReactor(
  private val emailService: SendEmailService,
) {
  companion object : Logging

  @Reactor
  suspend fun handle(e: UserRegistered) {
    emailService.sendEmailConfirmationEmail(e.displayName, e.email, e.aggregatorGuid.toString())
  }
}
```

- метод проектора должен быть в Spring-бине
- должна быть аннотация @Reactor
- в классе может быть несколько методов -- ограничений нет
- первый аргумент -- событие
- второй (опционально) -- метаданные события
- метод должен быть suspend (в принципе, это ограничение можно снять, но сейчас так в движке и не планирую использовать не suspend-методы)
- исключение в реакторе НЕ отменит сохранение события и запуск других реакторов

## Чтение данных

Чтение данных основной проекции -- никаких ограничений, как обычно.

Так же доступно чтение событий:

```kotlin
interface EventStoreReader {

  fun <T : DomainEvent> findEventsSinceId(
    eventIdFrom: Long,
    aggregator: String? = null,
    aggregatorGuid: UUID? = null,
    accountGuid: AccountGuid? = null,
    eventTypes: List<String>? = null,
    maxBatchSize: Int? = null,
  ): Flow<DomainEventWithIdAndMeta<T>>

  fun <T : DomainEvent> findEventsSinceGuid(
    eventGuidFrom: UUID,
    aggregator: String? = null,
    aggregatorGuid: UUID? = null,
    accountGuid: AccountGuid? = null,
    eventTypes: List<String>? = null,
    maxBatchSize: Int? = null,
  ): Flow<DomainEventWithIdAndMeta<T>>

  fun <T : DomainEvent> findEventsSinceDate(
    date: LocalDateTime,
    aggregator: String? = null,
    aggregatorGuid: UUID? = null,
    accountGuid: AccountGuid? = null,
    eventTypes: List<String>? = null,
    maxBatchSize: Int? = null,
  ): Flow<DomainEventWithIdAndMeta<T>>

  fun <T : DomainEvent> findEvents(
    aggregator: String? = null,
    aggregatorGuid: UUID? = null,
    accountGuid: AccountGuid? = null,
    eventTypes: List<String>? = null,
    maxBatchSize: Int? = null,
  ): Flow<DomainEventWithIdAndMeta<T>>

}
```

Это API можно использовать для получения истории или для создания асинхронных проекций.

Потенциально можно написать и свое API чтения событий, в jOOQ все для этого есть.

Так же можно делать полную или частичную перегенерацию базы (аргументы старта приложения или кастомный код).

Пример получения истории (естественно, можно смешивать чтение из событий и из основной проекции, т.к. это все в даже одной базе):

```kotlin
@Service
class DebugService(
  private val eventStoreReader: EventStoreReader,
) {
  suspend fun getUserAudit(userGuid: UUID): List<String> {
    return eventStoreReader.findEvents<UserEvent>("user", userGuid, maxBatchSize = 100)
      .map { (id, event, meta) ->
        when (event) {
          is UserMetaUpdated -> "updated $event"
          is UserRegistered -> "user registered with id $id ${meta.createdAt} $event"
          is UserRemoved -> "user deleted at ${meta.createdAt}"
        }
    }
  }
}
```

Тут в API немного некрасиво -- нет связи "user" и UserEvent. Возможно, имеет смысл передавать базовый класс, но он абстрактный. Если у кого-то есть идеи как лучше сделать API (без строчки "user" и без приведения "as UserEvent") -- будут рад прочитать.

## Ограничения

- В данной реализации Event Bus не внедрен (для трансляции событий через какую-нибудь Кафку или NATS), но ничего не мешает такое прикрутить, если кому-нибудь будет нужно.

## Итог

Кода немного больше за счет выделения отдельной абстракции -- Событие. Так же время уходит на саму абстракцию -- назвать, выделить поля и т.п.

Для CRUD получается больше кода, но круда не так много как может показаться -- нужно приучить себя думать в событиях бизнес-области, а не создать/удалить запись в таблице базы данных.

В целом, мне нравится, поэтому и решил поделиться с сообществом.

## Tech stack

- Kotlin 1.8
- Spring Boot 3 (reactive with Kotlin co-routines)
- Spring Data Repositories & jOOQ
- JUnit 5 with mockk
- Java 17
- Postgres
- Docker

## Dev links

- App: http://localhost:8080/
- Dev UI: http://localhost:8081/actuator
- Swagger spec json: http://localhost:8080/v3/api-docs
- Swagger spec yaml: http://localhost:8080/v3/api-docs.yaml
- Swagger UI: http://localhost:8080/swagger-ui.html
- GraphQL endpoint: http://localhost:8080/graphql/
- GraphQL schema: http://localhost:8080/graphql/schema.graphql
- GraphQL UI: http://localhost:8080/graphiql
- Health liveness: http://localhost:8081/actuator/health/liveness
- Health readiness: http://localhost:8081/actuator/health/readiness
- Generic metrics: http://localhost:8081/actuator/metrics/disk.free
- Prometheus metrics: http://localhost:8081/actuator/prometheus
- Config props: http://localhost:8081/actuator/configprops
- Env variables: http://localhost:8081/actuator/env
- Log settings: http://localhost:8081/actuator/loggers
- DB migrations info: http://localhost:8081/actuator/flyway

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./bin/start-postgres
./bin/generate-flyway
./bin/generate-jooq
./bin/run-dev
```

## Packaging and running the application

The application can be packaged using:

```shell script
./bin/build-docker
```
