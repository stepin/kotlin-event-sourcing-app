package name.stepin.exception

open class DomainException(message: String = "") : RuntimeException(message) {
    constructor(errorCode: ErrorCode) : this(errorCode.toString())
}
