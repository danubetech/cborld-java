package com.danubetech.cborld;

public class CborLdException extends RuntimeException {

    public enum CborLdError {
        ERR_NOT_CBORLD,
        ERR_UNDEFINED_COMPRESSED_CONTEXT,
        ERR_INVALID_TERM_DEFINITION,
        ERR_UNDEFINED_TERM,
        ERR_UNKNOWN_CBORLD_TERM,
        ERR_UNKNOWN_CBORLD_TERM_ID,
        ERR_INVALID_ENCODED_CONTEXT
    };

    private CborLdError cborLdError;

    public CborLdException(CborLdError cborLdError) {
        this.cborLdError = cborLdError;
    }

    public CborLdException(CborLdError cborLdError, String message) {
        super(message);
        this.cborLdError = cborLdError;
    }

    public CborLdException(CborLdError cborLdError, String message, Throwable cause) {
        super(message, cause);
        this.cborLdError = cborLdError;
    }

    public CborLdException(CborLdError cborLdError, Throwable cause) {
        super(cause);
        this.cborLdError = cborLdError;
    }

    public CborLdException(CborLdError cborLdError, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.cborLdError = cborLdError;
    }

    public CborLdError getCborLdError() {
        return this.cborLdError;
    }
}
