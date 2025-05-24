
# 🕹️ STAG - Simple Text Adventure Game Engine

Welcome to my implementation of **STAG (Simple Text Adventure Game)** — a general-purpose socket-server-based engine for running rich, configurable text adventure games inspired by classics like _Zork_.

This repository contains a fully working game engine that:
- Can interpret and run any valid game scenario defined via entity and action config files.
- Supports natural language-style user input with flexible command parsing.
- Operates as a networked server allowing multiplayer sessions.
- Fully adheres to academic constraints, including **no lambdas, arrays, ArrayLists, ternary operators, string concatenation, or unqualified method calls**.

> 🔒 **All code in this repository is written solely by me, without AI assistance or collaboration, and follows all academic integrity guidelines.**

---

## 🚀 Features

- ✅ Reads game world structure from `.dot` files and dynamic game rules from `.xml` action files.
- ✅ Implements built-in commands (`look`, `goto`, `get`, `drop`, `inventory`, `health`).
- ✅ Supports flexible and decorated commands (e.g., `please chop the tree using the axe`).
- ✅ Prevents invalid or ambiguous actions with clear feedback.
- ✅ Multiplayer-capable with per-player inventory, location, and health management.
- ✅ Robust error handling with persistent game state during each session.

---

## 🛠️ Tech Stack

- **Java 17**
- **Maven**
- **JPGD DOT Parser** for entity parsing
- **JAXP (DOM Parser)** for XML action files
- **JUnit 5** for testing

---

## 🧪 Running the Game

### ⚙️ Compile and Run

Make sure you're in the root directory of the Maven project:

```bash
./mvnw clean install
```

### 🖥️ Launch the Server

```bash
./mvnw exec:java@server
```

By default, the server loads `basic-entities.dot` and `basic-actions.xml`. You can modify the server constructor to load any custom game config files.

### 💬 Connect a Client

In a new terminal window:

```bash
./mvnw exec:java@client -Dexec.args="playerName"
```

Replace `playerName` with your desired player name (e.g., `simon`).

---

## 🧾 Example Commands

Try out these commands once connected:

```
look
goto forest
get axe
chop tree with axe
inv
health
```

Commands are case-insensitive, word-order agnostic, and allow natural phrasing:

```
please use the axe to chop the tree
```

---

## 👩‍💻 Development Notes

- All illegal Java constructs (as listed in the brief) are strictly avoided.
- The project is validated using the provided `strange` checker tool:

```bash
./mvnw exec:java@strange -Dexec.args=src/main/java/edu/uob/GameServer.java
```

- Configuration files and test cases are stored under `src/test/resources`.

---

## ✅ Tests

Run the full suite of automated tests with:

```bash
./mvnw test
```

Or use IntelliJ’s test runner for selective execution.

---

## 📷 Demo

> A short gameplay demo (without audio) is available [here](03%20Game%20Engine/video/adventure.mp4).

---

## 📁 Project Structure

```
src/
├── main/java/edu/uob/        # Game engine source code
├── test/java/edu/uob/        # Unit and integration tests
├── resources/config/         # Sample entity and action files
└── libs/                     # External libraries (JPGD, etc.)
```

---

## 📬 Contact

Feel free to explore the code and fork the repository. If you have questions, reach out via [GitHub Issues](https://github.com/ShrirangL/STAG/issues) or connect with me on [LinkedIn]([https://www.linkedin.com](https://www.linkedin.com/in/shrirang-lokhande-b402bb15b/)).

---
