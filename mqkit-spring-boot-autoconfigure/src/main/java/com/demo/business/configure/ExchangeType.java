package com.demo.business.configure;

/**
 * @author zbm
 * @date 2023/2/113:32
 */
public enum ExchangeType {
    DIRECT("direct"),
    FANOUT("fanout"),
    TOPIC("topic");

    private String desc;

    private ExchangeType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return this.desc;
    }
}
