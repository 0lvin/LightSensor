all: lightsensor-native

lightsensor-native: lightsensor.c
	gcc lightsensor.c -I include/ -o lightsensor-native --pedantic -Wall

clean:
	rm *.o lightsensor-arm lightsensor-native -f

lightsensor-arm: lightsensor.c
	arm-linux-gnueabihf-gcc --static lightsensor.c -I include/ -o lightsensor-arm --pedantic -Wall
