package com.codeaudit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示通用的 AST 节点
 */
public class UniversalASTNode {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("language")
    private String language;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("file")
    private String file;
    
    @JsonProperty("package")
    private String packageName;
    
    @JsonProperty("startLine")
    private int startLine;
    
    @JsonProperty("endLine")
    private int endLine;
    
    @JsonProperty("metadata")
    private Map<String, String> metadata;
    
    @JsonProperty("relations")
    private List<Relation> relations;
    
    @JsonProperty("fullClassName")
    private String fullClassName;
    
    @JsonProperty("methodParams")
    private List<String> methodParams;
    
    @JsonProperty("isInnerClass")
    private boolean isInnerClass;
    
    @JsonProperty("outerClass")
    private String outerClass;
    
    @JsonProperty("fields")
    private List<FieldInfo> fields;
    
    @JsonProperty("superClasses")
    private List<ClassRef> superClasses;
    
    @JsonProperty("subClasses")
    private List<ClassRef> subClasses;

    public UniversalASTNode() {
        this.metadata = new HashMap<>();
        this.relations = new ArrayList<>();
        this.methodParams = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.superClasses = new ArrayList<>();
        this.subClasses = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public void setRelations(List<Relation> relations) {
        this.relations = relations;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    public List<String> getMethodParams() {
        return methodParams;
    }

    public void setMethodParams(List<String> methodParams) {
        this.methodParams = methodParams;
    }

    public boolean isInnerClass() {
        return isInnerClass;
    }

    public void setInnerClass(boolean innerClass) {
        isInnerClass = innerClass;
    }

    public String getOuterClass() {
        return outerClass;
    }

    public void setOuterClass(String outerClass) {
        this.outerClass = outerClass;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public List<ClassRef> getSuperClasses() {
        return superClasses;
    }

    public void setSuperClasses(List<ClassRef> superClasses) {
        this.superClasses = superClasses;
    }

    public List<ClassRef> getSubClasses() {
        return subClasses;
    }

    public void setSubClasses(List<ClassRef> subClasses) {
        this.subClasses = subClasses;
    }
}

