install:
	mvn clean install

move:
	mkdir -p servers;
	cd servers;
	mkdir -p servers/server1;
	mkdir -p servers/server2;
	mkdir -p servers/server3;
	cp Application/target/demo.jar servers/server1;
	cp Application/target/demo.jar servers/server2;
	cp Application/target/demo.jar servers/server3;

run:
	mvn clean install;
	cp Application/target/demo.jar servers/server1;
	cp Application/target/demo.jar servers/server2;
	cp Application/target/demo.jar servers/server3;
	cd servers/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server1 && java -jar demo.jar  2>&1 |tee log.txt