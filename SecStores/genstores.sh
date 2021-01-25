#!/bin/sh

clear_()
{
	rm keystore*
	rm *.cer
	rm truststore
	rm ../utils/src/main/resources/keystore*
	rm ../utils/src/main/resources/truststore
	rm ../utils/src/main/resources/*.cer
}
setup()
{
	printf 'setting up keys for %d parties\n' "$1"
	PASS=testpass
	TRUST=truststore
	for INDEX in $(seq "$1")
	do 
		ALIAS=P$INDEX
		STORE=keystore$INDEX
		keytool -genkeypair -alias "$ALIAS" -keyalg RSA -validity 600 -keystore "$STORE" -dname "CN=$ALIAS, OU=IFX, O=IAIK, C=AT" -storepass $PASS
		keytool -export -alias "$ALIAS" -keystore "$STORE" -rfc -file "$ALIAS".cer -storepass $PASS
		keytool -import -alias "$ALIAS"-cert -file "$ALIAS".cer -keystore truststore -storepass $PASS -noprompt
	done
}
move()
{
	cp keystore* ../utils/src/main/resources
	cp *.cer ../utils/src/main/resources
	cp truststore ../utils/src/main/resources
}

clear_
setup "$1"
move
