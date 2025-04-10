package com.francelabs.datafari.rag;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Message {
    // Role should be "user", "system" or "assistant"
    private static final List<String> ALLOWED_ROLES = Arrays.asList("user", "system", "assistant", "developer");

    String role;
    String content;

    public Message(String role, String content) {
        if (!ALLOWED_ROLES.contains(role)) role = "user";
        this.role = role;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) role = "user";
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(role, message.role) && Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
}
