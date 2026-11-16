package edu.lmu.unified.room;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private final List<Room> rooms = List.of(

            new Room(
                    101L,
                    "Conference Room A",
                    "University Hall",
                    50
            ),

            new Room(
                    102L,
                    "Conference Room B",
                    "University Hall",
                    30
            ),

            new Room(
                    103L,
                    "Technology Lab",
                    "Seaver Science Center",
                    40
            ),

            new Room(
                    104L,
                    "Multipurpose Room",
                    "Malone Student Center",
                    100
            ),

            new Room(
                    105L,
                    "Meeting Room",
                    "William H. Hannon Library",
                    20
            )
    );

    // Retrieve all campus rooms.
    public List<Room> findAll() {
        return rooms;
    }

    // Retrieve a campus room by ID.
    public Optional<Room> findById(Long id) {

        return rooms.stream()
                .filter(room -> room.id().equals(id))
                .findFirst();
    }

    // Check whether a campus room exists.
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }
}