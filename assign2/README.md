# Compile and Run Instructions

## Requirements
- Ensure you have Java SE 21 (or a more recent version) installed.


## Compilation Instructions

1. Navigate to the 'src' folder inside the 'assign2' folder.
2. Run the following command to compile the program:
```bash
javac *.java utils/*.java game/*.java game/words/Words.java
```


## Run Instructions

### Starting the Server

1. Navigate to the 'src' folder inside the 'assign2' folder.
2. Run the following command to start the server:
```bash
java Server <port> 
```

### Starting the Client
1. Navigate to the 'src' folder inside the 'assign2' folder.
2. Run the following command to start the client:
```bash
java Client <port>
```

### Login information
There are three accounts that are created by default. This information is stored in a file called `database.txt`. If this file does not exist, it will be created automatically when we run the server.

- username: tiago
- password: tiago123
---
- username: joao
- password: joao123
---
- username: henrique
- passoword: henrique123
