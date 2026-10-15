package pl.cheapskountant.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxTransitionTest {
    @Test
    fun noNetworkStaysWaiting() {
        assertEquals(
            OutboxTransitions.WAITING,
            OutboxTransitions.statusAfter(SendOutcome.NoNetwork, OutboxTransitions.READY),
        )
    }

    @Test
    fun httpErrorsAndMissingStatusBecomeError() {
        listOf(400, 401, 422, 500, 503, 504).forEach { code ->
            assertEquals(
                OutboxTransitions.ERROR,
                OutboxTransitions.statusAfter(SendOutcome.HttpError(code, "not authorized"), OutboxTransitions.READY),
            )
        }
        assertEquals(
            OutboxTransitions.ERROR,
            OutboxTransitions.statusAfter(SendOutcome.NoStatus, OutboxTransitions.READY),
        )
    }

    @Test
    fun successUsesTheRequestedStatus() {
        assertEquals(
            OutboxTransitions.READY,
            OutboxTransitions.statusAfter(SendOutcome.Success("json"), OutboxTransitions.READY),
        )
    }

    @Test
    fun resubmitRestoresThePreviousPhase() {
        assertEquals(
            OutboxTransitions.TRANSCRIBE to OutboxTransitions.WAITING,
            OutboxTransitions.resubmitReceipt(OutboxTransitions.TRANSCRIBE),
        )
        assertEquals(
            OutboxTransitions.CONFIRM to OutboxTransitions.WAITING,
            OutboxTransitions.resubmitReceipt(OutboxTransitions.CONFIRM),
        )
        assertEquals(OutboxTransitions.WAITING, OutboxTransitions.resubmitExpense())
    }

    @Test
    fun deleteIsRefusedWhileSendingAndErrorsDoNotSend() {
        assertFalse(OutboxTransitions.canDelete(OutboxTransitions.SENDING))
        assertTrue(OutboxTransitions.canDelete(OutboxTransitions.WAITING))
        assertTrue(OutboxTransitions.canDelete(OutboxTransitions.READY))
        assertTrue(OutboxTransitions.canDelete(OutboxTransitions.ERROR))
        assertFalse(OutboxTransitions.canSend(OutboxTransitions.ERROR))
        assertTrue(OutboxTransitions.canSend(OutboxTransitions.WAITING))
    }
}
