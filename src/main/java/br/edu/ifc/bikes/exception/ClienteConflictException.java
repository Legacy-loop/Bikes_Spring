package br.edu.ifc.bikes.exception;

public class ClienteConflictException extends RuntimeException {
    public ClienteConflictException(String message) {
        super(message);
    }
}
