package pl.cheapskountant.mobile.domain

sealed interface SendOutcome<out T> {
    data class Success<T>(val body: T?) : SendOutcome<T>
    data class HttpError(val code: Int, val reason: String?) : SendOutcome<Nothing>
    data object NoStatus : SendOutcome<Nothing>
    data object NoNetwork : SendOutcome<Nothing>
}
