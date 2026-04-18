package logger

import (
	"log"
	"os"
)

type Logger struct {
	base  *log.Logger
	level string
}

func New(level string) *Logger {
	return &Logger{
		base:  log.New(os.Stdout, "[netbridge-agent] ", log.LstdFlags|log.Lmicroseconds),
		level: level,
	}
}

func (l *Logger) Infof(format string, args ...any) {
	l.base.Printf("[INFO] "+format, args...)
}

func (l *Logger) Errorf(format string, args ...any) {
	l.base.Printf("[ERROR] "+format, args...)
}

func (l *Logger) Debugf(format string, args ...any) {
	if l.level == "debug" {
		l.base.Printf("[DEBUG] "+format, args...)
	}
}
