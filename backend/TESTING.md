
# LMU Unified — Backend Testing Guide

## Requirements

- Java 17 or later
- Docker Desktop
- Maven Wrapper (included in the project)

## Redis Integration Test Setup

The room reservation repository integration tests require
a dedicated Redis instance running on localhost port 6380.

Start the Redis test container:

```bash
docker run -d --name lmu-unified-redis-test -p 127.0.0.1:6380:6379 redis:7-alpine
```

If the container already exists but is stopped, run:

```bash
docker start lmu-unified-redis-test
```

Verify that Redis is running:

```bash
docker exec lmu-unified-redis-test redis-cli ping
```

Expected response:

```text
PONG
```

## Run the Complete Backend Test Suite

Navigate to the backend directory:

```bash
cd backend
```

On Windows:

```powershell
.\mvnw.cmd clean test
```

On macOS or Linux:

```bash
./mvnw clean test
```

## Expected Results

As of SCRUM-27, the complete backend test suite contains
107 tests.

```text
Tests run: 107, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Room Reservation Test Coverage

The room reservation tests verify:

- Valid room reservation creation.
- Approved event and organizer ownership validation.
- Authentication and organizer role authorization.
- Room catalog retrieval.
- Overlapping reservation detection.
- Prevention of double-booking.
- Nonoverlapping and adjacent reservations.
- Reservations in different rooms.
- Reservations on different dates.
- Pending and cancelled reservation handling.
- Concurrent reservation requests.
- Atomic Redis conflict detection and persistence.

## Redis Test Isolation

The repository integration tests use Redis database 15
on localhost port 6380.

Test cleanup is restricted to room reservation keys in
the dedicated test database.

Do not configure these integration tests to use a
production Redis instance.

## Stop the Test Container

After testing, the Redis container can be stopped:

```bash
docker stop lmu-unified-redis-test
```

Restart it before running the integration tests again.
