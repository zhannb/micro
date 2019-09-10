package com.code.user.interceptor.exception;

/**
 * create by liuliang
 * on 2019-08-08  11:14
 */
public class SqlProcessInterceptorException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public SqlProcessInterceptorException(String message) {
        super(message);
    }

    public SqlProcessInterceptorException(Throwable throwable) {
        super(throwable);
    }

    public SqlProcessInterceptorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
