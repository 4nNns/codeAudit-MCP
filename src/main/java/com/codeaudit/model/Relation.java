package com.codeaudit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 表示节点间的关系
 */
public class Relation {
    @JsonProperty("target_id")
    private String targetId;
    
    @JsonProperty("type")
    private String type;

    public Relation() {
    }

    public Relation(String targetId, String type) {
        this.targetId = targetId;
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

