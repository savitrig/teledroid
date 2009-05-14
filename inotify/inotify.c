#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/inotify.h>
#include <errno.h>

int main(int argc, char *argv[])
{
    int c;
    int nfd, ffd;
    int res;
	char event_buf[512];
    struct inotify_event *event;
	int event_mask = IN_ALL_EVENTS;
    int event_count = 1;
	int print_files = 0;
	int verbose = 2;
	int width = 80;
	char **file_names;
	int file_count;
	int id_offset = 0;
	int i;
	char *buf;

    do {
        c = getopt(argc, argv, "m:c:pv:w:");
        if (c == EOF)
            break;
        switch (c) {
        case 'm':
            event_mask = strtol(optarg, NULL, 0);
            break;
        case 'c':
            event_count = atoi(optarg);
            break;
		case 'p':
			print_files = 1;
			break;
        case 'v':
            verbose = atoi(optarg);
            break;
        case 'w':
            width = atoi(optarg);
            break;
        case '?':
            printf("%s: invalid option -%c\n",
                argv[0], optopt);
            exit(1);
        }
    } while (1);

	printf("iNotify is now running. init::");
	for(i=0;i<argc; i++)
		printf(" %s", argv[i]);
	printf("\n");
	int ipoint = 0;
	
    if (argc <= optind) {
        printf("Usage: %s [-m eventmask] [-c count] [-p] [-v verbosity] path [path ...]\n", argv[0]);
		return 1;
    }
	printf(">>Monitor Point %d\n", ipoint++);

    nfd = inotify_init();
    if(nfd < 0) {
        printf("inotify_init failed, %s\n", strerror(errno));
        return 1;
    }
	file_names = argv + optind;
	file_count = argc - optind;
	for(i = 0; i < file_count; i++) {
		res = inotify_add_watch(nfd, file_names[i], event_mask);
		if(res < 0) {
	        printf("inotify_add_watch failed for %s, %s\n", file_names[i], strerror(errno));
			return 1;
		}
		if(i == 0)
			id_offset = -res;
		if(res + id_offset != i) {
			printf("%s got unexpected id %d instead of %d\n", file_names[i], res, i);
			return 1;
		}
	}
	printf(">>Monitor Point %d\n", ipoint++);

	buf = malloc(width + 2);
	printf(">>Monitor Point %d\n", ipoint++);
	
	int icount = 0, ocount = 0;
	
    while(1) {
		if(ocount++<7)
			printf("\tOutter count: %d\n", ocount);
		int event_pos = 0;
        res = read(nfd, event_buf, sizeof(event_buf));
        if(res < (int)sizeof(*event)) {
			if(errno == EINTR)
				continue;
            printf("could not get event, %s\n", strerror(errno));
            return 1;
        }
		//printf("got %d bytes of event information\n", res);
		while(res >= (int)sizeof(*event)) {
			if(icount++<7)
				printf("\t\tInner count: %d\n", icount);
			
			int event_size;
			event = (struct inotify_event *)(event_buf + event_pos);
			if(verbose >= 2)
		        printf("%s: %08x %08x \"%s\"\n", file_names[event->wd + id_offset], event->mask, event->cookie, event->len ? event->name : "");
			else if(verbose >= 2)
		        printf("%s: %08x \"%s\"\n", file_names[event->wd + id_offset], event->mask, event->len ? event->name : "");
			else if(verbose >= 1)
		        printf("%d: %08x \"%s\"\n", event->wd, event->mask, event->len ? event->name : "");
			if(print_files && (event->mask & IN_MODIFY)) {
				char filename[512];
				ssize_t read_len;
				char *display_name;
				int buflen;
				strcpy(filename, file_names[event->wd + id_offset]);
				if(event->len) {
					strcat(filename, "/");
					strcat(filename, event->name);
				}
				ffd = open(filename, O_RDONLY);
				display_name = (verbose >= 2 || event->len == 0) ? filename : event->name;
				buflen = width - strlen(display_name);
				read_len = read(ffd, buf, buflen);
				if(read_len > 0) {
					if(read_len < buflen && buf[read_len-1] != '\n') {
						buf[read_len] = '\n';
						read_len++;
					}
					if(read_len == buflen) {
						buf[--read_len] = '\0';
						buf[--read_len] = '\n';
						buf[--read_len] = '.';
						buf[--read_len] = '.';
						buf[--read_len] = '.';
					}
					else {
						buf[read_len] = '\0';
					}
					printf("%s: %s", display_name, buf);
				}
				close(ffd);
			}
	        if(event_count && --event_count == 0)
	            return 0;
			event_size = sizeof(*event) + event->len;
			res -= event_size;
			event_pos += event_size;
		}
    }

    return 0;
}
