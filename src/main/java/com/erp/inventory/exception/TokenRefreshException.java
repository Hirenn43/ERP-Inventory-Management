package com.erp.inventory.exception;

/** Thrown by the token-refresh flow when the supplied token is not a usable refresh token
 *  (wrong {@code type} claim, or failed validation). Maps to 401 with its own message -
 *  distinct from {@link org.springframework.security.authentication.BadCredentialsException},
 *  whose 401 body is deliberately kept generic to avoid credential enumeration. */
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message) {
        super(message);
    }
}
