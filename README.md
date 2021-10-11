![Safe-DEED Logo](https://github.com/Safe-DEED/SLTBP/blob/master/Safe-DEED_logo.png)

# Secure Lead-Time Based Pricing (SLTBP)
For a detailed description of this crypthograpic protocol and a possible use-case see https://safe-deed.eu/wp-content/uploads/2020/06/Safe-DEED_D5_4.pdf

## Installation

The demonstrator is built using the following dependencies. 

- [Maven](https://maven.apache.org/)
- [FRESCO](https://github.com/aicis/fresco)

We first need maven, which in turn downloads the required FRESCO version. 
For Linux, the dependencies can be installed with the following commands. 
First, we need jdk, maven, and make.

```console
sudo apt-get update && sudo apt-get install -y \
     openjdk-11-jdk \
     maven \
     make
```
Then we can install FRESCO and dependencies using maven.
```
mvn clean install 
```
This Project is written and tested in Linux.
In general, the project is compatible with windows. The testing procedure and quick build setup are using Makefile.
Hence, the testing and demonstration procedure are not compatible with windows.


## Important make targets

Let us briefly describe the most important make targets. 

`make all`: This target calls `make install`, `make move` and finally `make run`

`make install`: This make target calls `mvn clean install` 

`make move`: This target creates a `servers` directory and a subdirectory for each of the parties.
We use this directory structure to imitate different instances locally. This has to be executed once before running our demonstration setup.

`make run`: This target executes the setup from the `servers` directory.

`make {linear, concave, convex, bucket}`: These targets copy the current executable in the demonstration directories. 
Then the demonstrator is run with the given setting. (i.e. either linear, convex, concave or bucket)

**Note: each server that is executed, needs the ATPUnits and the NetworkConfig JSON files**

Here is a short overview of the instructions used, such that different setups can be easily created.

 `mvn clean install;`

Here the project is compiled using maven.
 
 `cp Application/target/demo.jar servers/serverX;`

The created jar file needs to be copied to the directory of each server.

 `cd servers/serverX && java -jar demo.jar 2>&1 |tee log.txt` 

Finally the different instances are started. 

## Demonstrator run
With our `make run` target we provide a demonstrator run with predefined inputs.
We can modify the input to the protocol in the `ATPUnits.json` files for each participant individually.

The individual instances print their outputs to the `log.txt` files in their respective directory.
More importantly, however, the result of the protocol is stored in the `accepted_order.json`. An empty json object represents a failed deal, while the accepted orders are stored in there otherwhise.
The clients can see their accepted order, while the server sees all accepted orders.

The settings of the demonstrator are set in two data files.

`MPCSettings.json` defines all parameters set in FRESCO as well as runtime specifics. Those specifics
are a debug mode, a logging mode, a benchmark mode and the selected pricing algorithm.

`NetworkConfig.json` defines the IP address and Port of each individual party. 


## Running over the network

When running this demonstrator over the network, only one instance of `Application/target/demo.jar` has to be started. In the same directory
however, the necessary JSON files have to be placed. With the Network Configuration, we can define the IP and Port of each participant. 
The individual certificates of the other parties and the truststore need to be in the same directory. In our setup they are included 
in the utils resources and are compiled into the jar.

## Acknowledgements

This project has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 825225.
