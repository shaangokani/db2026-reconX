package com.dbtraining.reconx.exception;

/** TICKET-ADV072 — 401 Unauthorized: login email/password did not match. */
public class InvalidCredentialsException extends ReconException {
    public InvalidCredentialsException(String message) { super(message); }
}
