package com.echoflow.chat;

/**
 * 聊天消息数据模型。
 */
public class Message {
    public final String role;    // user | assistant
    public String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
