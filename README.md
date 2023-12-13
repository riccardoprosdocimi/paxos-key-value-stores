# Multiple key-value stores that reach consensus using Paxos
> A fault-tolerant architecture to achieve consensus of updates amongst replicated state machine KV-store nodes using Paxos.
### It's set up to allow multiple concurrent clients to communicate with any of the servers and perform three basic operations:
- PUT(key, value)
- GET(key)
- DELETE(key)
- - -
#### Usage (locally):
1) Open up two terminal windows and navigate to `/Project4/src`
2) In one window, type `javac node/*.java utils/*.java main/PaxosServerCreator.java` (hit <kbd>↩</kbd>), followed by `java main.PaxosServerCreator` (hit <kbd>↩</kbd>)
3) The nodes are now running
4) In the other window, type `javac client/*.java utils/*.java main/ClientMain.java` (hit <kbd>↩</kbd>), followed by `java main.ClientMain <hostname> <port>` (hit <kbd>↩</kbd>)
5) The client is now running
6) The predefined protocol is:
    * `PUT:key:value`(hit <kbd>↩</kbd>)
    * `GET:key`(hit <kbd>↩</kbd>)
    * `DELETE:key`(hit <kbd>↩</kbd>)
7) To pre-populate the nodes, type `pp` (hit <kbd>↩</kbd>)
8) To shut down the application, type `stop` (hit <kbd>↩</kbd>) or `shutdown` (hit <kbd>↩</kbd>)
- - -
<details>
    <summary>SIDE NOTES</summary>
        <ul>
            <li>Each node has a 10% chance of randomly failing in the prepare and accept phases.</li>
            <li>Other failure cases are the following:</li>
                <ul>
                    <li>A client tries to delete a nonexistent key.</li>
                    <li>A client tries to add an already existent key.</li>
                </ul>
            <li>At boot up, the user has to specify which node to connect to.</li>
        </ul>
</details>
