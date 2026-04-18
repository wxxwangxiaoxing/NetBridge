//go:build linux

package monitor

import (
	"bufio"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
)

func memoryDescription() string {
	total := readMemInfoValue("MemTotal")
	if total == 0 {
		return "unknown"
	}
	return humanizeKB(total)
}

func diskDescription() string {
	total, _, err := diskBytes(".")
	if err != nil || total == 0 {
		return "unknown"
	}
	return humanizeBytes(total)
}

func memoryUsagePercent() float64 {
	total := readMemInfoValue("MemTotal")
	available := readMemInfoValue("MemAvailable")
	if total == 0 || available == 0 || available > total {
		return 0
	}
	used := total - available
	return float64(used) * 100 / float64(total)
}

func diskUsagePercent() float64 {
	total, free, err := diskBytes(".")
	if err != nil || total == 0 || free > total {
		return 0
	}
	used := total - free
	return float64(used) * 100 / float64(total)
}

func loadAverage() float64 {
	content, err := os.ReadFile("/proc/loadavg")
	if err != nil {
		return 0
	}
	fields := strings.Fields(string(content))
	if len(fields) == 0 {
		return 0
	}
	value, _ := strconv.ParseFloat(fields[0], 64)
	return value
}

func networkBytes(receive bool) int64 {
	file, err := os.Open("/proc/net/dev")
	if err != nil {
		return 0
	}
	defer file.Close()

	var total int64
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if !strings.Contains(line, ":") {
			continue
		}
		parts := strings.SplitN(line, ":", 2)
		if len(parts) != 2 {
			continue
		}
		iface := strings.TrimSpace(parts[0])
		if iface == "lo" {
			continue
		}
		fields := strings.Fields(parts[1])
		if len(fields) < 16 {
			continue
		}
		index := 0
		if !receive {
			index = 8
		}
		value, _ := strconv.ParseInt(fields[index], 10, 64)
		total += value
	}
	return total
}

func readMemInfoValue(key string) int64 {
	file, err := os.Open("/proc/meminfo")
	if err != nil {
		return 0
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	prefix := key + ":"
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if !strings.HasPrefix(line, prefix) {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			return 0
		}
		value, _ := strconv.ParseInt(fields[1], 10, 64)
		return value
	}
	return 0
}

func diskBytes(path string) (uint64, uint64, error) {
	abs, err := filepath.Abs(path)
	if err != nil {
		return 0, 0, err
	}
	var stat syscall.Statfs_t
	if err := syscall.Statfs(abs, &stat); err != nil {
		return 0, 0, err
	}
	total := stat.Blocks * uint64(stat.Bsize)
	free := stat.Bavail * uint64(stat.Bsize)
	return total, free, nil
}
