package com.erp.inventory.exception;

/** Raised when a WhatsApp quick-entry payload could not be turned into a saved record. The message
 *  is user-facing - it is sent straight back to the WhatsApp sender. */
public class QuickEntryExecException extends RuntimeException {
    public QuickEntryExecException(String message) {
        super(message);
    }

    public QuickEntryExecException(String message, Throwable cause) {
        super(message, cause);
    }
}
