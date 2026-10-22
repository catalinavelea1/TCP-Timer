Implemented a TCP client-server system for managing countdown timers.
Both the server and client run within the same application, allowing
users to test socket communication locally on an Android emulator.

The interface provides separate controls for starting the server and
sending requests from the client. Once the server starts on a selected
port, the client connects to an IP address and port using a TCP socket.
Requests are sent as plain-text commands, and the server returns a text
response that is displayed in the application.

I separated the networking logic into dedicated thread classes so that
socket operations do not block the Android UI. ServerThread opens a
ServerSocket and accepts incoming connections. Each connection is
handled by a CommunicationThread, which reads the request and executes
the appropriate operation. ClientThread connects to the server, sends
the command, and reads its response. The application uses
BufferedReader and PrintWriter for socket I/O and posts UI updates
back to the main thread.

-- timer management --
The server supports three commands:
-set,x,y creates a timer lasting x minutes and y seconds.
-poll checks whether the timer has expired.
-reset removes the timer associated with the client.

I represent each timer with a Kotlin data class containing its duration
and start time in milliseconds. When a timer is set, the server records
SystemClock.elapsedRealtime(). A later poll compares the elapsed
time with the requested duration, avoiding errors around clock changes
and removing the need for an external time service.

The server stores timers in a ConcurrentHashMap, using the client's IP
address as the key. This allows multiple communication threads to access
the shared timer data. The implementation therefore associates one timer
with each IP address, rather than with each individual socket connection.

For compatibility with the original command protocol, poll returns
inactive while the timer is still counting down and active after it
expires. If no timer exists, it returns none. A successful set
returns ok, while reset returns reset done.

I used Kotlin's when expression to dispatch incoming commands and
toIntOrNull() to parse timer values without throwing a number-format
exception. Invalid parameters and unsupported commands receive an error
response. Nullable socket references, exception handling, and resource
cleanup help the application handle failed connections more safely.
