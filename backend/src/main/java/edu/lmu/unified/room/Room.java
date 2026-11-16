package edu.lmu.unified.room;

public record Room(
        Long id,
        String name,
        String location,
        int capacity
) {
}