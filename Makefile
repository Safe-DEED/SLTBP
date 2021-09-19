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
	mkdir -p linear;
	cd linear;
	mkdir -p linear/server1;
	mkdir -p linear/server2;
	mkdir -p linear/server3;
	cp Application/target/demo.jar linear/server1;
	cp Application/target/demo.jar linear/server2;
	cp Application/target/demo.jar linear/server3;
	cd linear/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd linear/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd linear/server1 && java -jar demo.jar  2>&1 |tee log.txt

convex:
	mkdir -p convex;
	cd convex;
	mkdir -p convex/server1;
	mkdir -p convex/server2;
	mkdir -p convex/server3;
	cp Application/target/demo.jar convex/server1;
	cp Application/target/demo.jar convex/server2;
	cp Application/target/demo.jar convex/server3;
	cd convex/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd convex/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd convex/server1 && java -jar demo.jar  2>&1 |tee log.txt

concave:
	mkdir -p concave;
	cd concave;
	mkdir -p concave/server1;
	mkdir -p concave/server2;
	mkdir -p concave/server3;
	cp Application/target/demo.jar concave/server1;
	cp Application/target/demo.jar concave/server2;
	cp Application/target/demo.jar concave/server3;
	cd concave/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd concave/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd concave/server1 && java -jar demo.jar  2>&1 |tee log.txt

bucket:
	mkdir -p bucket;
	cd bucket;
	mkdir -p bucket/server1;
	mkdir -p bucket/server2;
	mkdir -p bucket/server3;
	cp Application/target/demo.jar bucket/server1;
	cp Application/target/demo.jar bucket/server2;
	cp Application/target/demo.jar bucket/server3;
	cd bucket/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd bucket/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd bucket/server1 && java -jar demo.jar  2>&1 |tee log.txt
