//go:build !linux

package monitor

func memoryDescription() string {
	return "unknown"
}

func diskDescription() string {
	return "unknown"
}

func memoryUsagePercent() float64 {
	return 0
}

func diskUsagePercent() float64 {
	return 0
}

func loadAverage() float64 {
	return 0
}

func networkBytes(receive bool) int64 {
	return 0
}
