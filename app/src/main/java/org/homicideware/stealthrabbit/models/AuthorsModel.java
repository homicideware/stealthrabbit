package org.homicideware.stealthrabbit.models;

public class AuthorsModel {

    private final String nickname;
    private final String name;
    private final String github;
    private final String telegram;
    private final String description;

    public AuthorsModel(String nickname, String name, String github, String telegram, String description) {
        this.nickname = nickname;
        this.name = name;
        this.github = github;
        this.telegram = telegram;
        this.description = description;
    }

    public String getNickname() {
        return nickname;
    }

    public String getNicknameDesc() {
        return name;
    }

    public String getGithub() {
        return github;
    }

    public String getTelegram() {
        return telegram;
    }

    public String getDescription() {
        return description;
    }
}
