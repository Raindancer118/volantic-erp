package de.volantic.erp.core.numberrange;

/** Thrown when a number is requested for a range key that has not been defined. */
public class NumberRangeNotDefinedException extends RuntimeException {

    public NumberRangeNotDefinedException(String key) {
        super("number range not defined: " + key);
    }
}
