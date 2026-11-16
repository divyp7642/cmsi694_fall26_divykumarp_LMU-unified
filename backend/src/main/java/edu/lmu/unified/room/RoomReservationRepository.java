
package edu.lmu.unified.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class RoomReservationRepository {

    private static final String RESERVATION_KEY_PREFIX =
            "lmu:room-reservations:";

    private static final String RESERVATION_IDS_KEY =
            "lmu:room-reservations:ids";

    private static final String RESERVATION_SEQUENCE_KEY =
            "lmu:room-reservations:sequence";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final DefaultRedisScript<Long>
            CONFIRM_RESERVATION_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local function timeToNanos(time)

                        local hours, minutes, seconds, fraction =
                            string.match(
                                time,
                                '^(%d%d):(%d%d):(%d%d)%.(%d+)$'
                            )

                        if not hours then

                            hours, minutes, seconds =
                                string.match(
                                    time,
                                    '^(%d%d):(%d%d):(%d%d)$'
                                )

                        end

                        if not hours then

                            hours, minutes =
                                string.match(
                                    time,
                                    '^(%d%d):(%d%d)$'
                                )

                            seconds = '0'

                        end

                        if not hours then
                            error('Invalid reservation time: ' .. time)
                        end

                        local nanos = 0

                        if fraction then

                            local padded =
                                fraction .. '000000000'

                            nanos = tonumber(
                                string.sub(padded, 1, 9)
                            )

                        end

                        return (
                            tonumber(hours) * 3600
                            + tonumber(minutes) * 60
                            + tonumber(seconds)
                        ) * 1000000000 + nanos

                    end

                    local requestedStart =
                        tonumber(ARGV[4])

                    local requestedEnd =
                        tonumber(ARGV[5])

                    local reservationIds =
                        redis.call('SMEMBERS', KEYS[1])

                    for _, id in ipairs(reservationIds) do

                        local existingJson =
                            redis.call(
                                'GET',
                                ARGV[1] .. id
                            )

                        if existingJson then

                            local existing =
                                cjson.decode(existingJson)

                            if existing.status == 'CONFIRMED'
                                and tostring(existing.roomId)
                                    == ARGV[2]
                                and existing.date == ARGV[3]
                            then

                                local existingStart =
                                    timeToNanos(existing.startTime)

                                local existingEnd =
                                    timeToNanos(existing.endTime)

                                if existingStart < requestedEnd
                                    and requestedStart < existingEnd
                                then
                                    return 0
                                end

                            end

                        end

                    end

                    redis.call(
                        'SET',
                        KEYS[2],
                        ARGV[6]
                    )

                    redis.call(
                        'SADD',
                        KEYS[1],
                        ARGV[7]
                    )

                    return 1
                    """,
                    Long.class
            );

    public RoomReservationRepository(
            StringRedisTemplate redisTemplate
    ) {

        this.redisTemplate = redisTemplate;

        this.objectMapper = new ObjectMapper();

        this.objectMapper.registerModule(
                new JavaTimeModule()
        );

        this.objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );
    }

    // Generate a unique reservation ID.
    public Long generateReservationId() {

        Long sequence = redisTemplate.opsForValue()
                .increment(RESERVATION_SEQUENCE_KEY);

        if (sequence == null) {

            throw new IllegalStateException(
                    "Unable to generate reservation ID"
            );
        }

        return sequence;
    }

    // Atomically confirm a reservation only if no conflict exists.
    public boolean saveIfAvailable(
            RoomReservation reservation
    ) {

        if (reservation.status()
                != ReservationStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "Reservation must have CONFIRMED status"
            );
        }

        if (reservation.startTime() == null
                || reservation.endTime() == null
                || !reservation.startTime()
                        .isBefore(reservation.endTime())) {

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (reservation.id() == null
                || reservation.roomId() == null
                || reservation.date() == null) {

            throw new IllegalArgumentException(
                    "Reservation ID, room ID and date are required"
            );
        }

        try {

            String json =
                    objectMapper.writeValueAsString(reservation);

            String reservationKey =
                    RESERVATION_KEY_PREFIX + reservation.id();

            // Convert requested times into nanoseconds
            // since midnight for accurate numeric comparison.
            String startTimeNanos =
                    Long.toString(
                            reservation.startTime()
                                    .toNanoOfDay()
                    );

            String endTimeNanos =
                    Long.toString(
                            reservation.endTime()
                                    .toNanoOfDay()
                    );

            Long result = redisTemplate.execute(
                    CONFIRM_RESERVATION_SCRIPT,

                    List.of(
                            RESERVATION_IDS_KEY,
                            reservationKey
                    ),

                    RESERVATION_KEY_PREFIX,
                    reservation.roomId().toString(),
                    reservation.date().toString(),
                    startTimeNanos,
                    endTimeNanos,
                    json,
                    reservation.id().toString()
            );

            if (result == null) {

                throw new IllegalStateException(
                        "Unable to confirm room reservation"
                );
            }

            return result == 1L;

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to serialize room reservation",
                    exception
            );
        }
    }

    // Save non-confirmed reservations.
    // Confirmed reservations must use saveIfAvailable().
    public void save(RoomReservation reservation) {

        if (reservation.status()
                == ReservationStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "Use saveIfAvailable for confirmed reservations"
            );
        }

        try {

            String json =
                    objectMapper.writeValueAsString(reservation);

            String reservationKey =
                    RESERVATION_KEY_PREFIX + reservation.id();

            redisTemplate.opsForValue().set(
                    reservationKey,
                    json
            );

            redisTemplate.opsForSet().add(
                    RESERVATION_IDS_KEY,
                    reservation.id().toString()
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to serialize room reservation",
                    exception
            );
        }
    }

    // Retrieve a reservation by ID.
    public Optional<RoomReservation> findById(Long id) {

        String json = redisTemplate.opsForValue().get(
                RESERVATION_KEY_PREFIX + id
        );

        if (json == null) {
            return Optional.empty();
        }

        try {

            return Optional.of(
                    objectMapper.readValue(
                            json,
                            RoomReservation.class
                    )
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to deserialize room reservation",
                    exception
            );
        }
    }

    // Retrieve all room reservations.
    public List<RoomReservation> findAll() {

        var reservationIds =
                redisTemplate.opsForSet().members(
                        RESERVATION_IDS_KEY
                );

        if (reservationIds == null
                || reservationIds.isEmpty()) {

            return List.of();
        }

        return reservationIds.stream()
                .map(Long::valueOf)
                .sorted()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }

    // Retrieve reservations for a specific room and date.
    public List<RoomReservation> findByRoomIdAndDate(
            Long roomId,
            LocalDate date
    ) {

        return findAll().stream()
                .filter(reservation ->
                        roomId.equals(reservation.roomId())
                )
                .filter(reservation ->
                        date.equals(reservation.date())
                )
                .toList();
    }

    // Retrieve only confirmed reservations.
    public List<RoomReservation> findConfirmedByRoomIdAndDate(
            Long roomId,
            LocalDate date
    ) {

        return findByRoomIdAndDate(roomId, date)
                .stream()
                .filter(reservation ->
                        reservation.status()
                                == ReservationStatus.CONFIRMED
                )
                .toList();
    }
}
