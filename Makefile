.PHONY: all install move run linear convex concave bucket

all: install move run

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
	cd servers/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd servers/server1 && java -jar demo.jar  2>&1 |tee log.txt

linear:
	cp Application/target/demo.jar linear/server1;
	cp Application/target/demo.jar linear/server2;
	cp Application/target/demo.jar linear/server3;
	cd linear/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd linear/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd linear/server1 && java -jar demo.jar  2>&1 |tee log.txt

convex:
	cp Application/target/demo.jar convex/server1;
	cp Application/target/demo.jar convex/server2;
	cp Application/target/demo.jar convex/server3;
	cd convex/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd convex/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd convex/server1 && java -jar demo.jar  2>&1 |tee log.txt

concave:
	cp Application/target/demo.jar concave/server1;
	cp Application/target/demo.jar concave/server2;
	cp Application/target/demo.jar concave/server3;
	cd concave/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd concave/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd concave/server1 && java -jar demo.jar  2>&1 |tee log.txt

bucket:
	cp Application/target/demo.jar bucket/server1;
	cp Application/target/demo.jar bucket/server2;
	cp Application/target/demo.jar bucket/server3;
	cd bucket/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd bucket/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd bucket/server1 && java -jar demo.jar  2>&1 |tee log.txt
