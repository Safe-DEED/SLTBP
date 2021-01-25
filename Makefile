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

run-big:
	mvn clean install;
	mkdir -p "servers/server1";
	mkdir -p "servers/server2";
	mkdir -p "servers/server3";
	mkdir -p "servers/server4";
	mkdir -p "servers/server5";
	mkdir -p "servers/server6";
	mkdir -p "servers/server7";
	mkdir -p "servers/server8";
	mkdir -p "servers/server9";
	mkdir -p "servers/server10";
	mkdir -p "servers/server11";
	mkdir -p "servers/server12";
	mkdir -p "servers/server13";
	cp Application/target/demo.jar servers/server1;
	cp Application/target/demo.jar servers/server2;
	cp Application/target/demo.jar servers/server3;
	cp Application/target/demo.jar servers/server4;
	cp Application/target/demo.jar servers/server5;
	cp Application/target/demo.jar servers/server6;
	cp Application/target/demo.jar servers/server7;
	cp Application/target/demo.jar servers/server8;
	cp Application/target/demo.jar servers/server9;
	cp Application/target/demo.jar servers/server10;
	cp Application/target/demo.jar servers/server11;
	cp Application/target/demo.jar servers/server12;
	cp Application/target/demo.jar servers/server13;
	cd servers/server13 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server12 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server11 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server10 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server9 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server8 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server7 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server6 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server5 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server4 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server1 && java -jar demo.jar  2>&1 |tee log.txt
