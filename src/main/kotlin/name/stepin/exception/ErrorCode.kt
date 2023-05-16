package name.stepin.exception

enum class ErrorCode(val code: Int, val message: String) {
    USER_NOT_FOUND(1, "Пользователь не найдена"),
    ACCOUNT_NOT_FOUND(2, "Аккаунт не найдена"),
    USER_ALREADY_REGISTERED(3, "Пользователь уже зарегистрирован"),
}
