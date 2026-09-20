package com.lifeos.ai.llm;

public class LlmException extends RuntimeException {

    private final boolean retryable;

    public LlmException(String message) {
        super(message);
        this.retryable = false;
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = false;
    }

    public LlmException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
