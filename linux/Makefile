all: lightsensor-x86 lightsensor-arm

lightsensor-x86: lightsensor.c
	gcc lightsensor.c -I include/ -o lightsensor-x86 --pedantic -Wall

clean:
	rm *.o lightsensor-arm lightsensor-x86 -f

lightsensor-arm: lightsensor.c
	arm-linux-gnueabihf-gcc --static lightsensor.c -I include/ -o lightsensor-arm --pedantic -Wall
