# STOMP World Cup Incidents (WCI) — Assignment 3 SPL

A client-server system for real-time World Cup event reporting and summarization, built on the [STOMP protocol](https://stomp.github.io/stomp-specification-1.2.html).

The **server** is written in Java and supports two concurrency models. The **client** is a multithreaded C++ application that connects to the server, subscribes to game channels, reports live events, and generates match summaries.

---

## Project Structure

```
.
├── server/          # Java Maven project — STOMP server
│   └── src/main/java/bgu/spl/net/
│       ├── api/     # Interfaces: MessagingProtocol, MessageEncoderDecoder
│       ├── impl/
│       │   ├── stomp/   # StompServer, StompMessagingProtocolImpl, ConnectionsImpl
│       │   └── data/    # Database (singleton), User, LoginStatus
│       └── srv/     # Server infrastructure: Reactor, TPC, BaseServer
│
├── client/          # C++ STOMP client
│   ├── src/
│   │   ├── StompClient.cpp      # Main entry point, CLI loop
│   │   ├── StompProtocol.cpp    # Handles incoming server frames
│   │   ├── ClientState.cpp      # Subscription & event state management
│   │   ├── ConnectionHandler.cpp
│   │   └── event.cpp            # JSON event parsing
│   ├── include/
│   └── data/
│       └── events1.json         # Sample event data
│
└── data/
    └── sql_server.py            # SQLite helper script
```

---

## Prerequisites

**Server**
- Java 11+
- Maven 3.6+

**Client**
- GCC with C++17 support
- `make`
- Boost libraries (for socket I/O)

---

## Building & Running

### Server

```bash
cd server
mvn compile

# Thread-per-client mode
mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" -Dexec.args="7777 tpc"

# Reactor mode (3 worker threads)
mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" -Dexec.args="7777 reactor"
```

### Client

```bash
cd client
make
./bin/StompWCIClient
```

---

## Client Commands

Once the client is running, use these commands interactively:

| Command | Description |
|---|---|
| `login {host:port} {username} {password}` | Connect to the STOMP server |
| `join {channel}` | Subscribe to a game channel (e.g. `Germany_Japan`) |
| `exit {channel}` | Unsubscribe from a channel |
| `report {file.json}` | Parse a JSON events file and send all events to the server |
| `summary {channel} {username} {output_file}` | Write a match summary to a file |
| `logout` | Disconnect from the server |

**Example session:**

```
login 127.0.0.1:7777 userA passA
join Germany_Japan
report data/events1.json
summary Germany_Japan userA output.txt
logout
```

---

## Architecture

### Server — Two Concurrency Models

**Thread-per-client (TPC):** Each client connection is handled by a dedicated thread. Simple but scales poorly under heavy load.

**Reactor:** A single selector thread dispatches I/O events to a fixed thread pool. More scalable for many simultaneous connections.

Both models share the same `StompMessagingProtocolImpl` and `ConnectionsImpl`, which manage subscriptions and broadcast messages to channel subscribers.

### Client — Multithreaded

The client runs two threads concurrently:

- **Main thread** — reads user input from stdin and sends STOMP frames to the server.
- **Socket thread** — listens for incoming frames and delegates to `StompProtocol` for processing (CONNECTED, MESSAGE, RECEIPT, ERROR).

`ClientState` is the shared object between the two threads, holding login status, active subscriptions, and accumulated event data for summary generation.

### STOMP Frames Supported

| Frame (Client → Server) | Purpose |
|---|---|
| `CONNECT` | Authenticate with login/passcode |
| `SUBSCRIBE` | Join a topic/channel |
| `UNSUBSCRIBE` | Leave a channel |
| `SEND` | Publish an event message |
| `DISCONNECT` | Graceful logout |

| Frame (Server → Client) | Purpose |
|---|---|
| `CONNECTED` | Login confirmed |
| `MESSAGE` | Broadcast from a subscribed channel |
| `RECEIPT` | Acknowledgement of a client frame |
| `ERROR` | Protocol or auth error |

---

## DevContainer

A `.devcontainer/` configuration is included for VS Code Dev Containers or GitHub Codespaces. It sets up the full build environment automatically.

---

## Notes

- The server uses a singleton `Database` class to store registered users and their login state.
- Channel names are derived from team names in the event JSON (e.g. `Germany_Japan`).
- Event JSON files must follow the schema used in `client/data/events1.json`.
