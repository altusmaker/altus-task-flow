package com.amirdehi.simpletodo;

public enum TaskStatus {
    TODO("آماده انجام"), IN_PROGRESS("در حال انجام"), DONE("انجام شده");

    public final String label;

    TaskStatus(String label) { this.label = label; }
}
