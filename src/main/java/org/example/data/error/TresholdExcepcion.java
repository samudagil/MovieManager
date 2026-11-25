package org.example.data.error;

public class TresholdExcepcion extends RuntimeException{
    public TresholdExcepcion(String message){
        super(message);
    }

    public TresholdExcepcion() {
        super("El valor debe estar entre " + MINIMUMSIZE + " y " + MAXIMUMSIZE);
    }

    public static final int MAXIMUMSIZE = 100;
    public static final int MINIMUMSIZE = 0;
}
