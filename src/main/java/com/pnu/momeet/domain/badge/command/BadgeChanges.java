package com.pnu.momeet.domain.badge.command;

public record BadgeChanges(
    String name, boolean nameChanged,
    String description, boolean descChanged,
    boolean hasIconPart, String iconHash, boolean iconChanged
) {
    public boolean textChanged() { return nameChanged || descChanged; }
    public boolean nothingToDo() { return !textChanged() && (!hasIconPart || !iconChanged); }
}
