package com.security.rbac.utility;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class ListResponseStructure<T> {
    private int status;
    private String message;
    private List<T> data;
}

