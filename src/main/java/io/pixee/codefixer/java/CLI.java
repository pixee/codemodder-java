package io.pixee.codefixer.java;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

final class CLI {

  static ProgressBarBuilder createProgressBuilderBase() {
    return new ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setUnit("", 1)
        .setUpdateIntervalMillis(100)
        .setSpeedUnit(ChronoUnit.SECONDS)
        .startsFrom(0, Duration.ZERO)
        .setConsumer(new ConsoleProgressBarConsumer(System.err));
  }
}
