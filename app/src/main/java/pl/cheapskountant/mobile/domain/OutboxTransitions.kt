package pl.cheapskountant.mobile.domain

object OutboxTransitions {
    const val WAITING = "WAITING"
    const val SENDING = "SENDING"
    const val READY = "READY"
    const val ERROR = "ERROR"
    const val TRANSCRIBE = "TRANSCRIBE"
    const val CONFIRM = "CONFIRM"

    fun canDelete(status: String): Boolean = status != SENDING

    fun canSend(status: String): Boolean = status == WAITING

    fun statusAfter(outcome: SendOutcome<*>, successStatus: String): String = when (outcome) {
        is SendOutcome.Success -> successStatus
        SendOutcome.NoNetwork -> WAITING
        is SendOutcome.HttpError, SendOutcome.NoStatus -> ERROR
    }

    fun resubmitReceipt(phase: String): Pair<String, String> = phase to WAITING

    fun resubmitExpense(): String = WAITING
}
