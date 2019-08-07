package com.code.zuul.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * create by liuliang
 * on 2019-08-07  11:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Msg<T> implements Serializable {

    private static final long serialVersionUID = -1177183613782210351L;
    private Integer code;
    private String msg;
    private T data;
}
