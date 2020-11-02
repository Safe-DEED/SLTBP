![Safe-DEED Logo](https://github.com/Safe-DEED/SLTBP/blob/master/Safe-DEED_logo.png)

# Secure Lead-Time Based Pricing (SLTBP)
For a detailed description of this crypthograpic protocol and a possible use-case see https://safe-deed.eu/wp-content/uploads/2020/06/Safe-DEED_D5_4.pdf

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

Let us briefly describe the two make targets. 


On the one hand, there is `make move`. This target creates a `servers` directory and a subdirectory for each of the possible 8 different servers.
We use this directory structure to imitate different instances locally. This has to be executed once before running our demonstration setup.

On the other hand, we have `make run`. This target compiles the project using maven. All the necessary dependencies will be loaded automatically.
Then it runs the program on three different servers.

**Note: each server that is executed, needs the ATPUnits and the NetworkConfig JSON files**

Here is a short overview of the instructions used, such that different setups can be easily created.

 `mvn clean install;`

Here the project is compiled using maven.
 
 `cp Application/target/demo.jar servers/serverX;`

The created jar file needs to be copied to the directory of each server.

 `cd servers/serverX && java -jar demo.jar 2>&1 |tee log.txt` 

Finally the different instances are started. 

## Running over the network

When running this demonstrator over the network, only one instance of `Application/target/demo.jar` has to be started. In the same directory
however, the necessary JSON files have to be placed. With the Network Configuration, we can define the IP and Port of each participant. 
The individual certificates of the other parties and the truststore need to be in the same directory. In our setup they are included 
in the utils resources and are compiled into the jar.

## Acknowledgements

This project has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 825225.
