#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <linux/lightsensor.h>
#include <linux/input.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>


#define DEVICE_NAME 		"/dev/lightsensor"
#define EVENT_TYPE_LIGHT	ABS_MISC
void enable_sensor(int dev_fd, int en) {
	int err = 0;
	int flags = en ? 1 : 0;
	err = ioctl(dev_fd, LIGHTSENSOR_IOCTL_ENABLE, &flags);
	printf("Enable %d => %d\n", err, flags);
}

int get_status(int dev_fd) {
	int flags = 0;
	int err;
	err = ioctl(dev_fd, LIGHTSENSOR_IOCTL_GET_ENABLED, &flags);
	printf("Value %d => %d\n", err, flags);
	return flags;
}

double get_abs_value(int dev_fd) {
	struct input_absinfo absinfo;
	int err;
	int predefined_lux[10] = {
		10, 160, 225, 320, 640,
		1280, 2600, 5800, 8000, 10240
	};
	double lux = 0;
	err = ioctl(dev_fd, EVIOCGABS(EVENT_TYPE_LIGHT), &absinfo);
	if (absinfo.value >= 10) {
		lux = (double)predefined_lux[9] / 10.0 * absinfo.value;
	} else {
		lux = predefined_lux[absinfo.value];
	}

	printf("Value %d => %d, %.1f lux\n", err, absinfo.value, lux);
	return lux;
}

#define min_value(a, b) ((a) < (b)) ? (a) : (b)

/*
 * Parse list input devices from /proc/bus/input/devices
 */
char * get_device(const char * dev_name) {
	int fd = open("/proc/bus/input/devices", O_RDONLY);
	if (fd < 0) {
		printf("can't get input list\n");
		return NULL;
	}
	struct stat sb;
	int file_size = 0;
	if (fstat(fd, &sb) != 1) {
		file_size = sb.st_size;
	}
	if (!file_size)
		file_size = 256;
	char* buffer = malloc(file_size + 1);
	int read_done = 0;
	int res = 0;
	while ((res = read(fd, buffer + read_done, 256)) > 0) {
		read_done += res;
		if ((read_done + 256) > file_size) {
			buffer = realloc(buffer, file_size + 256);
			file_size += 256;
		}
	}
	close(fd);
	buffer[read_done + 1] = 0;
	if (res < 0) {
		printf("can't read\n");
		return NULL;
	}
	file_size = read_done;

	char name[129] = {0};
	char device[129] = {0};
	char * read_pos = buffer;
	while (read_pos < buffer + file_size) {
		char* end_pos = read_pos;
		// get end of line
		while ((end_pos < buffer + file_size) && (*end_pos != '\n'))
			end_pos ++;
		// check name
		if ((end_pos - read_pos) >= strlen("N: Name=")) {
			if (strncmp(read_pos, "N: Name=", strlen("N: Name=")) == 0) {
				read_pos += strlen("N: Name=");
				if (*read_pos == '"')
					read_pos ++;
				strncpy(name, read_pos, min_value(128, end_pos - read_pos));
				name[min_value(128, end_pos - read_pos)] = 0;
				if (name[min_value(128, end_pos - read_pos) -1] == '"')
					name[min_value(128, end_pos - read_pos) -1] = 0;
				read_pos = end_pos;
				//skip final \n
				if(read_pos < (buffer + file_size))
					read_pos ++;
				continue;
			}
		}
		// check handler
		if ((end_pos - read_pos) >= strlen("H: Handlers=")) {
			if (strncmp(read_pos, "H: Handlers=", strlen("H: Handlers=")) == 0) {
				read_pos += strlen("H: Handlers=");
				strncpy(device, read_pos, min_value(128, end_pos - read_pos));
				device[min_value(128, end_pos - read_pos)] = 0;
				read_pos = end_pos;
				//skip final \n
				if(read_pos < (buffer + file_size))
					read_pos ++;
				continue;
			}
		}
		if (((end_pos - read_pos) >= 1 && *read_pos == '\n') || ((end_pos - read_pos) == 0)) {
			if (strcmp(dev_name, name) == 0) {
				free(buffer);
				return strdup(device);
			}
			name[0] = 0;
			device[0] = 0;
			if(read_pos < (buffer + file_size))
				read_pos ++;
			continue;
		}
		read_pos = end_pos + 1;
	}
	free(buffer);
	return NULL;
}

int update_backlight(char* control, double value, int offset) {
	// try to open contol and update value
	int changed = 0;
	char file_name_buffer[256] = {0};
	sprintf(file_name_buffer, "/sys/class/%s/max_brightness", control);
	int max_file = open(file_name_buffer, O_RDONLY);
	sprintf(file_name_buffer, "/sys/class/%s/brightness", control);
	int value_file = open(file_name_buffer, O_WRONLY);
	if (max_file > 0 && value_file > 0) {
		char max_value_char[6] = {0};
		char value_char[6] = {0};
		if (read(max_file, max_value_char, 5)) {
			int max_value = strtol(max_value_char, NULL, 10);
			int for_set = max_value * value / 10240 + offset;
			if (for_set >= max_value) {
				for_set = max_value - 1;
			}
			sprintf(value_char, "%d", for_set);
			write(value_file, value_char, strlen(value_char));
			printf("Update '%s' to %s/%d\n", file_name_buffer, value_char, max_value);
			changed = 1;
		}
	} else {
		printf("Can't open backlight control '%s'\n", file_name_buffer);
	}
	close(max_file);
	close(value_file);
	return changed;
}

void update_lights(double value) {
	// update value for all known contols
	char backlight_dirs[][50] = {
		"backlight/acpi_video0",
		"leds/lcd-backlight"
	};
	int i, changed = 0;
	// update by virtual values like acpi
	for (i = 0; i < 2; i++) {
		if (update_backlight(backlight_dirs[i], value, 0))
			changed = 1;
	}
	// we can't change virtual control, try update by direct contol
	if (!changed) {
		char backlight_direct_dirs[][50] = {
			"backlight/radeon_bl0",
		};
		for (i = 0; i < 1; i++) {
			update_backlight(backlight_direct_dirs[i], value, 1);
		}
	}
}

int main() {
	int dev_fd = open(DEVICE_NAME, O_RDONLY);
	if (dev_fd >= 0) {
		if (!get_status(dev_fd))
			enable_sensor(dev_fd, 1);
	} else {
		printf("Light sensor swith has not found.\n");
	}
	double sum_light = 0;
	int count_light = 0;
	char * device = get_device("lightsensor-level");
	if (device) {
		printf("devices: %s\n", device);
		char one_dev[128] = "/dev/input/";
		char * read_pos = device;
		while (read_pos < (device + strlen(device))) {
			char * end_pos = read_pos;
			while ( *end_pos != 0 && *end_pos != ' ')
				end_pos ++;
			strncpy(one_dev + strlen("/dev/input/"), read_pos, min_value(127, end_pos - read_pos));
			one_dev[strlen("/dev/input/") + min_value(127, end_pos - read_pos) - 1] = 0;
			read_pos = end_pos + 1;
			int input_fd = open(one_dev, O_RDONLY);
			if (input_fd >= 0) {
				printf("Try use:'%s'\n", one_dev);
				sum_light += get_abs_value(input_fd);
				count_light ++;
			} else {
				printf("Cant access to '%s'\n", one_dev);
			}
		}
		free(device);
	} else {
		printf("Light sensor has not found.\n");
	}
	if (count_light) {
		sum_light = sum_light / count_light;
	}
	update_lights(sum_light);
	// flashlight
	// /sys/class/leds/flashlight/brightness
	// 0 - none
	// 1 - bottom
	// 2 - top
	// 3 - both
}
