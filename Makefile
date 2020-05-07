build-r: build run-m

build: install move

install: util client host application

util:
	cd utils && mvn clean install -DskipTests

client:
	cd Client && mvn clean install -DskipTests

host:
	cd Host && mvn clean install -DskipTests

application:
	cd Application && mvn clean install -DskipTests

move:
	mkdir -p servers;
	cd servers;
	mkdir -p servers/server1;
	mkdir -p servers/server2;
	mkdir -p servers/server3;
	mkdir -p servers/server4;
	mkdir -p servers/server5;
	mkdir -p servers/server6;
	mkdir -p servers/server7;
	mkdir -p servers/server8;
	cp Application/target/demo.jar servers/server1;
	cp Application/target/demo.jar servers/server2;
	cp Application/target/demo.jar servers/server3;
	cp Application/target/demo.jar servers/server4;
	cp Application/target/demo.jar servers/server5;
	cp Application/target/demo.jar servers/server6;
	cp Application/target/demo.jar servers/server7;
	cp Application/target/demo.jar servers/server8;

run:
	@cd servers/server1 && java -jar demo.jar -l -i 1 -H -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 1 --price 5  --volume 100 > log.txt 2>&1 &
	@cd servers/server2 && java -jar demo.jar -l -i 2    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 3 --price 5  --volume 10  > log.txt 2>&1 &
	@cd servers/server3 && java -jar demo.jar -l -i 3    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 2 --price 5  --volume 1   > log.txt 2>&1 &
	@cd servers/server4 && java -jar demo.jar -l -i 4    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 1 --price 5  --volume 20  > log.txt 2>&1 &
	@cd servers/server5 && java -jar demo.jar -l -i 5    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 6 --price 15  --volume 20 > log.txt 2>&1 &
	@cd servers/server6 && java -jar demo.jar -l -i 6    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 5 --price 5  --volume 7   > log.txt 2>&1 &
	@cd servers/server8 && java -jar demo.jar -l -i 8    -p 1:localhost:8071 -p 2:localhost:8072 -p 3:localhost:8073 -p 4:localhost:8074 -p 5:localhost:8075 -p 6:localhost:8076 -p 7:localhost:8077 -p 8:localhost:8078 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -Dspdz.obliviousTransfer=NAOR -A 1 -T 7 --price 6  --volume 1 2>&1 | tee log.txt

run-m:
	cd servers/server1 && java -jar demo.jar -l -i 1 -H -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 1 --price 5  --volume 100 > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar -l -i 2    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 9 --price 6  --volume 500 > log.txt 2>&1 &
	cd servers/server3 && java -jar demo.jar -l -i 3    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 8 --price 6  --volume 1 > log.txt 2>&1 &
	cd servers/server4 && java -jar demo.jar -l -i 4    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 4 --price 6  --volume 1 > log.txt 2>&1 &
	cd servers/server5 && java -jar demo.jar -l -i 5    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 6 --price 6 --volume 1 > log.txt 2>&1 &
	cd servers/server6 && java -jar demo.jar -l -i 6    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 2 --price 6  --volume 1 > log.txt 2>&1 &
	cd servers/server7 && java -jar demo.jar -l -i 7    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 1 --price 6  --volume 16 > log.txt 2>&1 &
	cd servers/server8 && java -jar demo.jar -l -i 8    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -p 4:localhost:8084 -p 5:localhost:8085 -p 6:localhost:8086 -p 7:localhost:8087 -p 8:localhost:8088 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -Dspdz.obliviousTransfer=DUMMY -A 1 -T 2 --price 6  --volume 1 2>&1 | tee log.txt

run-presentation-mascot:
	cd servers/server3 && java -jar demo.jar  -l -i 3 -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -A 1 -T 2 --price 5  --volume 100 > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar  -l -i 2 -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -A 1 -T 5 --price 10  --volume 1956 > log.txt 2>&1 &
	cd servers/server1 && java -jar demo.jar  -l -i 1 -H -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=MASCOT -A 1 -T 2 --price 1  --volume 10000  log.txt 2>&1

run-presentation-dummy:
	cd servers/server3 && java -jar demo.jar -l -i 3  -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -A 1 -T 1 --price 6  --volume 1000000 > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar -l -i 2    -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -A 1 -T 5 --price 5  --volume 100000 > log.txt 2>&1 &
	cd servers/server1 && java -jar demo.jar -l -i 1 -H   -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -Dspdz.modBitLength=128 -Dspdz.maxBitLength=10 -Dspdz.preprocessingStrategy=DUMMY -A 1 -T 2 --price 5  --volume 30 2>&1 | tee log.txt

#	mkdir -p servers;
#	cd servers;
#	mkdir -p servers/server1;
#	mkdir -p servers/server2;
#	mkdir -p servers/server3;
#	cp Application/target/demo.jar servers/server1;
#	cp Application/target/demo.jar servers/server2;
#	cp Application/target/demo.jar servers/server3;
