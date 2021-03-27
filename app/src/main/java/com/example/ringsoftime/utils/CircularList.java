package com.example.ringsoftime.utils;

/*
Doubly(prev, next) circular Linked List
*/
public class CircularList <T> {
    CircularListNode _node = null;
    int _size = 0;

    /*
    Add value to back of list, returning that node
    */
    public void add(T value) {
        CircularListNode<T> newNode = new CircularListNode<>(value);
        _size++;

        if(_node == null) {
            _node = newNode;
            _node.next = _node.previous = newNode;
        }

        CircularListNode<T> end = _node.previous;
        newNode.next = _node;
        _node.previous = newNode;
        newNode.previous = end;
        end.next = newNode;
    }

    /*
    Returns start of CircularList
    */
    public CircularListNode getFront() {
        return _node;
    }

    /*
    Returns end of CircularList
    */
    public CircularListNode getBack() {
        return _node.previous;
    }

    public int size() {
        return _size;
    }
}
