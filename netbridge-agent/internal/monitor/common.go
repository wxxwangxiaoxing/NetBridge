package monitor

import (
	"net"
	"os"
	"runtime"
	"strconv"

	"netbridge-agent/pkg/model"
)

func Snapshot() model.SystemSnapshot {
	hostname, _ := os.Hostname()
	return model.SystemSnapshot{
		Hostname: hostname,
		OS:       runtime.GOOS,
		Arch:     runtime.GOARCH,
		IP:       detectIP(),
		CPU:      cpuDescription(),
		Memory:   memoryDescription(),
		Disk:     diskDescription(),
	}
}

func Metrics() model.MetricsSnapshot {
	return model.MetricsSnapshot{
		MemoryUsage:     memoryUsagePercent(),
		DiskUsage:       diskUsagePercent(),
		NetworkInBytes:  networkBytes(true),
		NetworkOutBytes: networkBytes(false),
		LoadAverage:     loadAverage(),
	}
}

func detectIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		ipNet, ok := addr.(*net.IPNet)
		if !ok || ipNet.IP.IsLoopback() {
			continue
		}
		if ip4 := ipNet.IP.To4(); ip4 != nil {
			return ip4.String()
		}
	}
	return "127.0.0.1"
}

func Endpoint(host string, port int) string {
	if host == "" {
		host = "127.0.0.1"
	}
	return net.JoinHostPort(host, strconv.Itoa(port))
}

func cpuDescription() string {
	return runtime.GOARCH + " x" + strconv.Itoa(runtime.NumCPU())
}

func humanizeKB(kb int64) string {
	return humanizeBytes(uint64(kb) * 1024)
}

func humanizeBytes(value uint64) string {
	const unit = 1024
	if value < unit {
		return strconv.FormatUint(value, 10) + "B"
	}
	div, exp := uint64(unit), 0
	for n := value / unit; n >= unit; n /= unit {
		div *= unit
		exp++
	}
	suffixes := []string{"KB", "MB", "GB", "TB", "PB"}
	return strconv.FormatFloat(float64(value)/float64(div), 'f', 1, 64) + suffixes[exp]
}
