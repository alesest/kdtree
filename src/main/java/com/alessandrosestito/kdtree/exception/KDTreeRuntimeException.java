package com.alessandrosestito.kdtree.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class KDTreeRuntimeException extends RuntimeException {
    private final KDTreeExceptionType exceptionType;
    private final String description;
}
