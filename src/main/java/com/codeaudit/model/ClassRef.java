package com.codeaudit.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 表示类的引用（包名+类名）
 */
public class ClassRef {
    @JsonProperty("package")
    private String packageName;
    
    @JsonProperty("name")
    private String name;

    public ClassRef() {
    }

    public ClassRef(String packageName, String name) {
        this.packageName = packageName;
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassRef classRef = (ClassRef) o;
        return java.util.Objects.equals(packageName, classRef.packageName) &&
               java.util.Objects.equals(name, classRef.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(packageName, name);
    }
}

