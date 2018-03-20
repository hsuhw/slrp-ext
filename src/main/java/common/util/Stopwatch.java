package common.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Instant;

public final class Stopwatch
{
    private Stopwatch()
    {
    }

    public static long epochTimeInNs()
    {
        return System.currentTimeMillis() * 1000L;
    }

    public static long epochTimeInMs()
    {
        return System.currentTimeMillis();
    }

    public static long epochTimeInSec()
    {
        return Instant.now().getEpochSecond();
    }

    public static long currentThreadCpuTimeInNs()
    {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
    }

    public static long currentThreadCpuTimeInMs()
    {
        return currentThreadCpuTimeInNs() / 1000000L;
    }

    public static long currentThreadCpuTimeInSec()
    {
        return currentThreadCpuTimeInNs() / 1000000000L;
    }
}
