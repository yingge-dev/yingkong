#define _GNU_SOURCE
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

#define DATA_DIR "/data/adb/yingkong"
#define STATUS_PATH DATA_DIR "/status"
#define SAMPLE_PATH DATA_DIR "/samples.jsonl"
#define PID_PATH DATA_DIR "/pid"
#define MAX_CORES 16
#define MAX_FREQ 16
#define MAX_THERMAL 64
#define VERSION "0.2.0"

typedef struct {
    unsigned long long total;
    unsigned long long busy;
} CpuTimes;

static volatile sig_atomic_t running = 1;

static void on_signal(int sig) {
    (void)sig;
    running = 0;
}

static int read_text(const char *path, char *buf, size_t size) {
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return -1;
    ssize_t n = read(fd, buf, size - 1);
    close(fd);
    if (n < 0) return -1;
    buf[n] = 0;
    while (n > 0 && (buf[n - 1] == '\n' || buf[n - 1] == '\r' || buf[n - 1] == ' ')) {
        buf[--n] = 0;
    }
    return (int)n;
}

static long long read_number(const char *path, long long fallback) {
    char buf[128];
    if (read_text(path, buf, sizeof(buf)) < 0) return fallback;
    char *end = NULL;
    errno = 0;
    long long value = strtoll(buf, &end, 10);
    if (errno != 0 || end == buf) return fallback;
    return value;
}

static int read_cpu(CpuTimes *out, int *count) {
    FILE *file = fopen("/proc/stat", "r");
    if (!file) return -1;
    char line[512];
    int cores = 0;
    while (fgets(line, sizeof(line), file)) {
        if (strncmp(line, "cpu", 3) != 0) continue;
        unsigned long long user = 0, nice = 0, system = 0, idle = 0, iowait = 0;
        unsigned long long irq = 0, softirq = 0, steal = 0;
        if (line[3] == ' ') {
            if (sscanf(line + 3, "%llu %llu %llu %llu %llu %llu %llu %llu",
                       &user, &nice, &system, &idle, &iowait, &irq, &softirq, &steal) != 8) {
                continue;
            }
            unsigned long long busy = user + nice + system + irq + softirq + steal;
            out[0].busy = busy;
            out[0].total = busy + idle + iowait;
        } else if (isdigit((unsigned char)line[3])) {
            if (sscanf(line + 3, "%*d %llu %llu %llu %llu %llu %llu %llu %llu",
                       &user, &nice, &system, &idle, &iowait, &irq, &softirq, &steal) != 8) {
                continue;
            }
            if (cores + 1 < MAX_CORES) {
                unsigned long long busy = user + nice + system + irq + softirq + steal;
                out[cores + 1].busy = busy;
                out[cores + 1].total = busy + idle + iowait;
            }
            cores++;
        }
    }
    fclose(file);
    *count = cores;
    return 0;
}

static int read_meminfo(double *total_gb, double *avail_gb) {
    FILE *file = fopen("/proc/meminfo", "r");
    if (!file) return -1;
    char line[256];
    long long total_kb = 0, avail_kb = 0;
    while (fgets(line, sizeof(line), file)) {
        if (strncmp(line, "MemTotal:", 9) == 0) {
            sscanf(line + 9, "%lld", &total_kb);
        } else if (strncmp(line, "MemAvailable:", 13) == 0) {
            sscanf(line + 13, "%lld", &avail_kb);
        }
    }
    fclose(file);
    *total_gb = total_kb / 1024.0 / 1024.0;
    *avail_gb = avail_kb / 1024.0 / 1024.0;
    return 0;
}

static int read_thermal(double *max_c) {
    DIR *dir = opendir("/sys/class/thermal");
    if (!dir) return -1;
    double maximum = 0;
    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (strncmp(entry->d_name, "thermal_zone", 12) != 0) continue;
        char path[256];
        snprintf(path, sizeof(path), "/sys/class/thermal/%s/temp", entry->d_name);
        long long value = read_number(path, -1);
        if (value > 0) {
            double c = value / 1000.0;
            if (c > maximum && c < 150.0) maximum = c;
        }
    }
    closedir(dir);
    *max_c = maximum;
    return 0;
}

static int find_battery(char *out, size_t size) {
    const char *fallback = "/sys/class/power_supply/battery";
    char type[64];
    char path[256];
    snprintf(path, sizeof(path), "%s/type", fallback);
    if (read_text(path, type, sizeof(type)) < 0) {
        DIR *dir = opendir("/sys/class/power_supply");
        if (!dir) {
            snprintf(out, size, "%s", fallback);
            return 0;
        }
        struct dirent *entry;
        while ((entry = readdir(dir)) != NULL) {
            if (entry->d_name[0] == '.') continue;
            char candidate[256];
            snprintf(candidate, sizeof(candidate), "/sys/class/power_supply/%s/type", entry->d_name);
            if (read_text(candidate, type, sizeof(type)) >= 0 && strcmp(type, "Battery") == 0) {
                snprintf(out, size, "/sys/class/power_supply/%s", entry->d_name);
                closedir(dir);
                return 0;
            }
        }
        closedir(dir);
        snprintf(out, size, "%s", fallback);
        return 0;
    }
    snprintf(out, size, "%s", fallback);
    return 0;
}

static void read_battery(double *capacity, double *voltage, double *current_ma,
                         double *bat_temp, double *power_w) {
    char dir[256];
    find_battery(dir, sizeof(dir));
    char path[320];

    snprintf(path, sizeof(path), "%s/capacity", dir);
    *capacity = (double)read_number(path, 0);

    snprintf(path, sizeof(path), "%s/voltage_now", dir);
    long long voltage_uv = read_number(path, 0);
    if (voltage_uv <= 0) {
        snprintf(path, sizeof(path), "%s/batt_vol", dir);
        voltage_uv = read_number(path, 0) * 1000;
    }
    *voltage = voltage_uv / 1000000.0;

    snprintf(path, sizeof(path), "%s/current_now", dir);
    long long current_ua = read_number(path, 0);
    if (current_ua == 0) {
        snprintf(path, sizeof(path), "%s/batt_current", dir);
        current_ua = read_number(path, 0) * 1000;
    }
    *current_ma = current_ua / 1000.0;
    *power_w = (*voltage) * (current_ua < 0 ? -current_ua : current_ua) / 1000000.0;

    snprintf(path, sizeof(path), "%s/temp", dir);
    *bat_temp = read_number(path, 0) / 10.0;
}

static int read_cpu_freqs(int *mhz, int *count) {
    int n = 0;
    int maximum = 0;
    for (int cpu = 0; cpu < 32 && n < MAX_FREQ; cpu++) {
        char path[256];
        snprintf(path, sizeof(path),
                 "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq", cpu);
        long long khz = read_number(path, -1);
        if (khz <= 0) continue;
        int value = (int)(khz / 1000);
        mhz[n++] = value;
        if (value > maximum) maximum = value;
    }
    *count = n;
    return maximum;
}

static void read_gpu(double *busy, double *mhz) {
    *busy = (double)read_number("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage", 0);
    long long clock = read_number("/sys/class/kgsl/kgsl-3d0/clock_mhz", 0);
    if (clock > 0) {
        *mhz = (double)clock;
        return;
    }
    long long hz = read_number("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", 0);
    *mhz = hz > 0 ? hz / 1000000.0 : 0;
}

static void write_status(const char *json) {
    char tmp[64];
    snprintf(tmp, sizeof(tmp), "%s.tmp", STATUS_PATH);
    int fd = open(tmp, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (fd < 0) return;
    size_t length = strlen(json);
    ssize_t written = write(fd, json, length);
    close(fd);
    if (written == (ssize_t)length) rename(tmp, STATUS_PATH);
}

static void append_sample(const char *json) {
    struct stat st;
    if (stat(SAMPLE_PATH, &st) == 0 && st.st_size > 8 * 1024 * 1024) {
        rename(SAMPLE_PATH, DATA_DIR "/samples.1.jsonl");
    }
    FILE *file = fopen(SAMPLE_PATH, "a");
    if (!file) return;
    fputs(json, file);
    fputc('\n', file);
    fclose(file);
}

int main(void) {
    signal(SIGTERM, on_signal);
    signal(SIGINT, on_signal);
    signal(SIGHUP, on_signal);

    mkdir(DATA_DIR, 0755);

    long long old_pid = read_number(PID_PATH, -1);
    if (old_pid > 0 && kill((pid_t)old_pid, 0) == 0) {
        return 0;
    }
    char pid_text[32];
    snprintf(pid_text, sizeof(pid_text), "%d\n", getpid());
    int pid_fd = open(PID_PATH, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (pid_fd >= 0) {
        write(pid_fd, pid_text, strlen(pid_text));
        close(pid_fd);
    }

    setpriority(PRIO_PROCESS, 0, -20);

    CpuTimes previous[MAX_CORES];
    CpuTimes current[MAX_CORES];
    int core_count = 0;
    memset(previous, 0, sizeof(previous));
    read_cpu(previous, &core_count);
    sleep(1);

    while (running) {
        int now_cores = 0;
        read_cpu(current, &now_cores);

        double cpu_total = 0;
        if (now_cores > 0 && previous[0].total > 0) {
            unsigned long long total_delta = current[0].total - previous[0].total;
            unsigned long long busy_delta = current[0].busy - previous[0].busy;
            if (total_delta > 0) cpu_total = busy_delta * 100.0 / total_delta;
        }

        double cores[MAX_CORES];
        int core_values = now_cores < MAX_CORES ? now_cores : MAX_CORES;
        for (int i = 0; i < core_values; i++) {
            unsigned long long total_delta = current[i + 1].total - previous[i + 1].total;
            unsigned long long busy_delta = current[i + 1].busy - previous[i + 1].busy;
            cores[i] = total_delta > 0 ? busy_delta * 100.0 / total_delta : 0;
        }

        for (int i = 0; i < MAX_CORES; i++) previous[i] = current[i];

        double mem_total = 0, mem_avail = 0;
        read_meminfo(&mem_total, &mem_avail);
        double temp = 0;
        read_thermal(&temp);
        double capacity = 0, voltage = 0, current_ma = 0, bat_temp = 0, power = 0;
        read_battery(&capacity, &voltage, &current_ma, &bat_temp, &power);
        int freqs[MAX_FREQ];
        int freq_count = 0;
        int max_mhz = read_cpu_freqs(freqs, &freq_count);
        double gpu_busy = 0, gpu_mhz = 0;
        read_gpu(&gpu_busy, &gpu_mhz);

        char load[128] = "";
        read_text("/proc/loadavg", load, sizeof(load));

        struct timespec ts;
        clock_gettime(CLOCK_REALTIME, &ts);
        long long millis = (long long)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;

        char json[4096];
        int offset = snprintf(json, sizeof(json),
            "{\"ts\":%lld,\"pid\":%d,\"version\":\"%s\",\"cpu\":%.1f,\"cores\":[",
            millis, getpid(), VERSION, cpu_total);
        for (int i = 0; i < core_values; i++) {
            offset += snprintf(json + offset, sizeof(json) - offset, "%s%.0f", i ? "," : "", cores[i]);
        }
        offset += snprintf(json + offset, sizeof(json) - offset,
            "],\"mem_total_gb\":%.2f,\"mem_avail_gb\":%.2f,"
            "\"battery\":%.0f,\"voltage\":%.3f,\"current_ma\":%.0f,"
            "\"power\":%.2f,\"bat_temp\":%.1f,\"temp\":%.1f,"
            "\"cpu_max_mhz\":%d,\"cpu_freqs\":[",
            mem_total, mem_avail, capacity, voltage, current_ma, power, bat_temp, temp, max_mhz);
        for (int i = 0; i < freq_count; i++) {
            offset += snprintf(json + offset, sizeof(json) - offset, "%s%d", i ? "," : "", freqs[i]);
        }
        snprintf(json + offset, sizeof(json) - offset,
            "],\"gpu\":%.0f,\"gpu_mhz\":%.0f,\"load\":\"%s\"}",
            gpu_busy, gpu_mhz, load);

        write_status(json);
        append_sample(json);
        sleep(1);
    }

    unlink(PID_PATH);
    return 0;
}
