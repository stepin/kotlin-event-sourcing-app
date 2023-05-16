package name.stepin.domain.user.model

fun calcDisplayName(email: String, firstName: String?, lastName: String?): String {
    if (firstName == null && lastName == null) {
        return email
    }
    if (firstName == null) {
        return lastName!!
    }
    if (lastName == null) {
        return firstName
    }
    return "$firstName $lastName"
}
