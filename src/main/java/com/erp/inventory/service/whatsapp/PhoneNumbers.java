package com.erp.inventory.service.whatsapp;

/** Normalises phone numbers to digits-only E.164 (no '+', spaces or punctuation) so the webhook's
 *  {@code from} and a stored {@link com.erp.inventory.entity.WhatsappUserLink} phone compare equal. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String digits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }
}
