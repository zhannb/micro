package com.code.user.interceptor.enums;

/**
 * create by liuliang
 * on 2019-08-08  11:01
 */
public enum MethodPrefixEnum {
    GET("get", "get方法前缀"),
    SET("set", "set方法前缀");

    private String prefix;
    private String desc;

    private MethodPrefixEnum(String prefix, String desc) {
        this.prefix = prefix;
        this.desc = desc;
    }

    public void setPrefix(String prefix, String desc) {
        this.prefix = prefix;
        this.desc = desc;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getDesc() {
        return this.desc;
    }
}
