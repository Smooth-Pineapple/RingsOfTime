package com.example.ringsoftime.utils;

public class CircularListNode <T> {
    private T value;
    CircularListNode next;
    CircularListNode previous;

    CircularListNode(T value) { this.value = value; }

    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }

    public CircularListNode getNext() { return next; }
    public CircularListNode getPrevious() { return previous; }
}
