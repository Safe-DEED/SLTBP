# Secure Lead-Time Based Pricing (SLTBP)

## Dependencies

- [Maven](https://maven.apache.org/)
- [FRESCO](https://github.com/aicis/fresco)


## Installation
The current stable release of FRESCO is in the central maven repository. When using that, proceed as follows.

- Go to the main directory of the repository: `cd <project-directory>`
- Compile with dependencies: `mvn clean install`
- For subsequent compiling use: `make install`

The second statement will automatically download FRESCO and assemble our project into a jar file with dependencies. The third
one also prepares our directory structure for our demonstrator setups. The executable can be found in `Application/target/demo.jar`

## Important make targets

#### Setup

In our Makefile we provide several setups to try out our implementation. Let us briefly describe the general idea behind those setups.
There are setup targets and run targets in the Makefile. The most important setup is, as mentioned above, `make install`. First, this 
target compiles the program using maven, then, it creates a `servers` directory and also a subdirectory for 8 different servers. Finally
the compiled executable is copied into those server subdirectories. This way, we can simulate multiple parties on the network and 
have their respective log files in different folders.

#### Run

The run targets only differ in the number of parties and the selected preprocessing strategy. There will be a short overview
of the instructions used, such that different setups can be easily created.

 `cd servers/server1 && java -jar demo.jar -l -i 1 -H` 

 `-p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083` 

 `-DmodBitLength=128 -DmaxBitLength=10 -DpreprocessingStrategy=DUMMY`

 `-T 1 --price 5  --volume 100 > log.txt 2`
 
 - The first line enters the directory and executes the program. Then, it sets logging as active (deactivate by deleting ``-l``). With ``-i 1`` the id is set to one. This id has to be unique. With `-H` it is declared that this user is in fact the host of the protocol.
 - In the second line, all parties are specified, here localhost has to be replaced by the specific IP addresses of the clients. It is always necessary to specify your own settings, so the network manager can listen to the correct port.
 - The third line specifies the security parameters of FRESCO. Here the first two parameters should be kept the same, while the last one can also be set to 
 ``Strategy=MASCOT``. 
 > **Caution** The protocol can only fulfill the security claims, if MASCOT is used as a preprocessing strategy. Dummy should only be used to test the implementation.
 - In the final line there are the use case specific parameters. ``-T`` states the date, when the amount ordered is due. ``--price`` specified the suggested price for the amount. ``--volume`` specifies the amount requested in the transaction. The final statement piped the output into the logfile.