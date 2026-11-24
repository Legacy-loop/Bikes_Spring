package br.edu.ifc.bikes.exception;

/** Sinaliza uma operação de locação ou devolução incompatível com o estado atual. */
public class LocacaoConflictException extends RuntimeException {
    public LocacaoConflictException(String message) {
        super(message);
    }
}
