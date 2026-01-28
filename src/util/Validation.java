package util;

import exceptions.ValidationException;

public interface Validation<T> {
    void validate(T value) throws ValidationException;
}
