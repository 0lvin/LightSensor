#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <linux/lightsensor.h>
#include <linux/input.h>

#define DEVICE_NAME 		"/dev/lightsensor"
#define EVENT_TYPE_LIGHT	ABS_MISC
void enable_sensor(int dev_fd, int en) {
    int err = 0;
    int flags = en ? 1 : 0;
    err = ioctl(dev_fd, LIGHTSENSOR_IOCTL_ENABLE, &flags);
    printf("Enable %d => %d\n", err, flags);
}

void get_status(int dev_fd) {
    int flags = 0;
    int err;
    err = ioctl(dev_fd, LIGHTSENSOR_IOCTL_GET_ENABLED, &flags);
    printf("Value %d => %d\n", err, flags);
}

void get_abs_value(int dev_fd) {
    struct input_absinfo absinfo;
    int err;
    err = ioctl(dev_fd, EVIOCGABS(EVENT_TYPE_LIGHT), &absinfo);
    printf("Value %d => %d\n", err, absinfo.value);
}

int main() {
    int dev_fd = open(DEVICE_NAME, O_RDONLY);
    if (dev_fd < 0) {
	printf("No device!\n");
	return 0;
    }
    enable_sensor(dev_fd, 1);
    // get_status(dev_fd);
    // /sys/devices/virtual/input/input4/name == lightsensor-level
    int input_fd = open("/dev/input/event4", O_RDONLY);
    if (input_fd < 0) {
	printf("No input!\n");
	return 0;
    }
    get_abs_value(input_fd);
}
