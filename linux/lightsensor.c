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
#include <linux/videodev2.h>
#include <sys/mman.h>
#include <errno.h>


#define DEVICE_NAME			"/dev/lightsensor"
#define MAX_DEVICES			16
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

#define min_value(a, b) (((a) < (b)) ? (a) : (b))

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
			char * reallocated = realloc(buffer, file_size + 256);
			if (!reallocated) {
				break;
			}
			buffer = reallocated;
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
	sprintf(file_name_buffer, "/sys/%s/max_brightness", control);
	int max_file = open(file_name_buffer, O_RDONLY);
	if (max_file >= 0) {
		sprintf(file_name_buffer, "/sys/%s/brightness", control);
		int value_file = open(file_name_buffer, O_WRONLY);
		if (value_file >= 0) {
			char max_value_char[6] = {0};
			if (read(max_file, max_value_char, 5)) {
				char value_char[6] = {0};
				int max_value = strtol(max_value_char, NULL, 10);
				// can set only to devices wit real max
				if (max_value > 0) {
					int for_set = max_value * value / 10240 + offset;
					if (for_set >= max_value) {
						for_set = max_value - 1;
					}
					sprintf(value_char, "%d", for_set);
					write(value_file, value_char, strlen(value_char));
					printf("Update '%s' to %s/%d\n", file_name_buffer, value_char, max_value);
					changed = 1;
				}
			}
			close(value_file);
		} else {
			printf("Can't open backlight control '%s'\n", file_name_buffer);
		}
		close(max_file);
	} else {
		printf("Can't open backlight control '%s'\n", file_name_buffer);
	}
	return changed;
}

void update_lights(double value) {
	// update value for all known contols
	char backlight_dirs[][50] = {
		"devices/platform/backlight/backlight/backlight",
		"class/backlight/acpi_video0",
		"class/leds/lcd-backlight"
	};
	int i, changed = 0;
	// update by virtual values like acpi
	for (i = 0; i < 3; i++) {
		if (update_backlight(backlight_dirs[i], value, 0)) {
			changed = 1;
			break;
		}
	}
	// we can't change virtual control, try update by direct contol
	if (!changed) {
		char backlight_direct_dirs[50] = {0};
		for (i = 0; i < MAX_DEVICES; i++) {
			sprintf(backlight_direct_dirs, "backlight/radeon_bl%d", i);
			update_backlight(backlight_direct_dirs, value, 1);
		}
	}
}

int v4l_read_frame(int fd, const char * image_memmory, size_t full_size) {
	struct v4l2_buffer buf = {0};

	buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	buf.memory = V4L2_MEMORY_MMAP;

	if (ioctl(fd, VIDIOC_DQBUF, &buf) < 0) {
		return -1;
	}
	if (buf.index != 0) {
		return -1;
	}

	int light = 0;
	for (int i = 0; i < full_size; i += 2) {
		light += image_memmory[i] & 0xFF;
	}
	int result = 2.0 * light / full_size;

	if (ioctl(fd, VIDIOC_QBUF, &buf) < 0) {
		return -1;
	}

	return result;
}

int v4l_wait_shot(int fd) {
	for (;;) {
		fd_set fds;
		struct timeval tv;
		int select_result;

		FD_ZERO(&fds);
		FD_SET(fd, &fds);

		/* Timeout. */
		tv.tv_sec = 2;
		tv.tv_usec = 0;

		select_result = select(fd + 1, &fds, NULL, NULL, &tv);

		if (select_result == -1)
		{
			if (EINTR == errno)
				continue;
			return -1;
		}

		if (!select_result) {
			// can't get frame
			return -1;
		}
		return 0;
	}
}

void v4l_show_capabilities(char *device_name, int fd) {
	struct v4l2_capability cap = {0};
	// capabilities
	if (ioctl(fd, VIDIOC_QUERYCAP, &cap) < 0) {
		return;
	}

	printf("%s:\t", device_name);

	if (cap.capabilities & V4L2_CAP_VIDEO_CAPTURE) {
		printf("V4L2_CAP_VIDEO_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_OUTPUT) {
		printf("V4L2_CAP_VIDEO_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_OVERLAY) {
		printf("V4L2_CAP_VIDEO_OVERLAY,");
	}
	if (cap.capabilities & V4L2_CAP_VBI_CAPTURE) {
		printf("V4L2_CAP_VBI_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_VBI_OUTPUT) {
		printf("V4L2_CAP_VBI_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_SLICED_VBI_CAPTURE) {
		printf("V4L2_CAP_SLICED_VBI_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_SLICED_VBI_OUTPUT) {
		printf("V4L2_CAP_SLICED_VBI_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_RDS_CAPTURE) {
		printf("V4L2_CAP_RDS_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_OUTPUT_OVERLAY) {
		printf("V4L2_CAP_VIDEO_OUTPUT_OVERLAY,");
	}
	if (cap.capabilities & V4L2_CAP_HW_FREQ_SEEK) {
		printf("V4L2_CAP_HW_FREQ_SEEK,");
	}
	if (cap.capabilities & V4L2_CAP_RDS_OUTPUT) {
		printf("V4L2_CAP_RDS_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_CAPTURE_MPLANE) {
		printf("V4L2_CAP_VIDEO_CAPTURE_MPLANE,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_OUTPUT_MPLANE) {
		printf("V4L2_CAP_VIDEO_OUTPUT_MPLANE,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_M2M_MPLANE) {
		printf("V4L2_CAP_VIDEO_M2M_MPLANE,");
	}
	if (cap.capabilities & V4L2_CAP_VIDEO_M2M) {
		printf("V4L2_CAP_VIDEO_M2M,");
	}
	if (cap.capabilities & V4L2_CAP_TUNER) {
		printf("V4L2_CAP_TUNER,");
	}
	if (cap.capabilities & V4L2_CAP_AUDIO) {
		printf("V4L2_CAP_AUDIO,");
	}
	if (cap.capabilities & V4L2_CAP_RADIO) {
		printf("V4L2_CAP_RADIO,");
	}
	if (cap.capabilities & V4L2_CAP_MODULATOR) {
		printf("V4L2_CAP_MODULATOR,");
	}
	if (cap.capabilities & V4L2_CAP_SDR_CAPTURE) {
		printf("V4L2_CAP_SDR_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_EXT_PIX_FORMAT) {
		printf("V4L2_CAP_EXT_PIX_FORMAT,");
	}
	if (cap.capabilities & V4L2_CAP_SDR_OUTPUT) {
		printf("V4L2_CAP_SDR_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_META_CAPTURE) {
		printf("V4L2_CAP_META_CAPTURE,");
	}
	if (cap.capabilities & V4L2_CAP_READWRITE) {
		printf("V4L2_CAP_READWRITE,");
	}
	if (cap.capabilities & V4L2_CAP_ASYNCIO) {
		printf("V4L2_CAP_ASYNCIO,");
	}
	if (cap.capabilities & V4L2_CAP_STREAMING) {
		printf("V4L2_CAP_STREAMING,");
	}
	if (cap.capabilities & V4L2_CAP_META_OUTPUT) {
		printf("V4L2_CAP_META_OUTPUT,");
	}
	if (cap.capabilities & V4L2_CAP_TOUCH) {
		printf("V4L2_CAP_TOUCH,");
	}
	if (cap.capabilities & V4L2_CAP_IO_MC) {
		printf("V4L2_CAP_IO_MC,");
	}
	if (cap.capabilities & V4L2_CAP_DEVICE_CAPS) {
		printf("V4L2_CAP_DEVICE_CAPS,");
	}

	printf("\n");
}

int v4l_check_capabilities(int fd) {
	struct v4l2_capability cap = {0};
	int capabilities = V4L2_CAP_VIDEO_CAPTURE | V4L2_CAP_STREAMING;

	// capabilities
	if (ioctl(fd, VIDIOC_QUERYCAP, &cap) < 0) {
		return -1;
	}

	if (!((cap.capabilities & capabilities) == capabilities)) {
		return -1;
	}
	return 0;
}

int v4l_check_format(int fd) {
	struct v4l2_format fmt = {0};
	// set out format
	int width = 320;
	int height = 240;
	fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	fmt.fmt.pix.width = width;
	fmt.fmt.pix.height = height;
	fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_YUYV;
	fmt.fmt.pix.field = V4L2_FIELD_INTERLACED;
	if (ioctl(fd, VIDIOC_S_FMT, &fmt) < 0) {
		return -1;
	}
	// fix sizes
	int min;
	min = fmt.fmt.pix.width * 2; //YUYV take 2 bytes for 1 pixel
	if (fmt.fmt.pix.bytesperline < min)
		fmt.fmt.pix.bytesperline = min;
	min = fmt.fmt.pix.bytesperline * fmt.fmt.pix.height;
	if (fmt.fmt.pix.sizeimage < min)
		fmt.fmt.pix.sizeimage = min;

	if (fmt.fmt.pix.width != width)
		width = fmt.fmt.pix.width;

	if (fmt.fmt.pix.height != height)
		height = fmt.fmt.pix.height;
	printf("%dx%d shot\n", width, height);
	return 0;
}

int v4l_check_avaible_shots(int fd) {
	// check avaible shots
	struct v4l2_requestbuffers req = {0};
	req.count = 1;
	req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	req.memory = V4L2_MEMORY_MMAP;
	if (ioctl(fd, VIDIOC_REQBUFS, &req) < 0) {
		return -1;
	}
	printf("%d frames at once\n", req.count);
	return 0;
}

int v4l_mmap_frame(int fd, char ** image_memmory, int * buf_length) {
	if(!image_memmory || !buf_length) {
		return -1;
	}
	// try mmap
	struct v4l2_buffer buf = {0};
	buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	buf.memory = V4L2_MEMORY_MMAP;
	buf.index = 0; // frame buffer position

	if (ioctl(fd, VIDIOC_QUERYBUF, &buf) < 0) {
		return -1;
	}
	*buf_length = buf.length;
	*image_memmory = mmap(NULL /* start anywhere */ ,
		buf.length, PROT_READ | PROT_WRITE  /* required */ ,
		MAP_SHARED /* recommended */ ,
		fd, buf.m.offset
	);
	if (MAP_FAILED == image_memmory) {
		return -1;
	}
	printf("maped %d bytes\n", *buf_length);
	return 0;
}

int v4l_start_capture(int fd) {
	// start capture
	struct v4l2_buffer buf_capture = {0};
	buf_capture.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
	buf_capture.memory = V4L2_MEMORY_MMAP;
	buf_capture.index = 0;

	if (ioctl(fd, VIDIOC_QBUF, &buf_capture) < 0) {
		return -1;
	}
	// let's begin
	enum v4l2_buf_type type;
	type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

	if (ioctl(fd, VIDIOC_STREAMON, &type) < 0) {
		return -1;
	}

	return 0;
}

void v4l_stop_capture(int fd, char* image_memmory, int buf_length) {
	enum v4l2_buf_type type;
	type = V4L2_BUF_TYPE_VIDEO_CAPTURE;

	if (ioctl(fd, VIDIOC_STREAMOFF, &type) < 0) {
		printf("can't release device\n");
	}
	if (munmap(image_memmory, buf_length) < 0) {
		printf("can't free memmory\n");
	}
}

int v4l_cam_value(char*  device_name) {
	int fd = open(device_name, O_RDWR | O_NONBLOCK /* required */);

	if (fd < 0) {
		return -1;
	}

	v4l_show_capabilities(device_name, fd);

	if (v4l_check_capabilities(fd)) {
		return -1;
	}

	if (v4l_check_format(fd)) {
		return -1;
	}

	if(v4l_check_avaible_shots(fd)) {
		return -1;
	}

	int buf_length = 0;
	char* image_memmory = NULL;
	if(v4l_mmap_frame(fd, &image_memmory, &buf_length)) {
		return -1;
	}

	if(v4l_start_capture(fd)) {
		v4l_stop_capture(fd, image_memmory, buf_length);
		return -1;
	}

	int res = 0;
	if(v4l_wait_shot(fd) == 0) {
		res = v4l_read_frame(fd, image_memmory, buf_length);
	}

	v4l_stop_capture(fd, image_memmory, buf_length);

	close(fd);
	if (res >= 0) {
		return (res * 10240.0) / 64.0; //64 - because auto
	}

	return res;
}

int main() {
	int dev_fd = open(DEVICE_NAME, O_RDONLY);
	if (dev_fd >= 0) {
		if (!get_status(dev_fd))
			enable_sensor(dev_fd, 1);
	} else {
		printf("Light sensor switch has not found.\n");
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
	char camera_device[50] = {0};
	for(int i = 0; i < MAX_DEVICES; i++) {
		sprintf(camera_device, "/dev/video%d", i);
		int cam_value = v4l_cam_value(camera_device);
		if (cam_value >= 0) {
			printf("v4l value = %d\n", cam_value);
			sum_light += cam_value;
			count_light ++;
		}
	}

	if (count_light) {
		sum_light = sum_light / count_light;
	}

	printf("Light value = %f lux\n", sum_light);

	update_lights(sum_light);
	// flashlight
	// /sys/class/leds/flashlight/brightness
	// 0 - none
	// 1 - bottom
	// 2 - top
	// 3 - both
}
