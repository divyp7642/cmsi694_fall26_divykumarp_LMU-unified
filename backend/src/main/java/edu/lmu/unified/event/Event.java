package edu.lmu.unified.event;

public record Event(
        Long id,
        String title,
        String description,
        String date,
        String time,
        String location,
        String category
) {
}