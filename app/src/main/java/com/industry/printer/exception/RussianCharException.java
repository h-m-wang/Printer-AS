package com.industry.printer.exception;

public class RussianCharException extends Exception {

    @Override
    public String getMessage() {
        return "Permission Denied: Russian character not allowed";
    }

    @Override
    public Throwable getCause() {
        // TODO Auto-generated method stub
        return super.getCause();
    }
}
