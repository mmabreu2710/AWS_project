package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.io.FileWriter;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import java.io.PrintWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static long nblocks = 0;

    /**
     * Number of executed methods.
     */
    private static long nmethods = 0;

    /**
     * Number of executed instructions.
     */
    private static long ninsts = 0;

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
        clearStatistics();
    }

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void printStatistics() {
        System.out.println(String.format("[%s] Number of executed methods: %s", ICount.class.getSimpleName(), nmethods));
        System.out.println(String.format("[%s] Number of executed basic blocks: %s", ICount.class.getSimpleName(), nblocks));
        System.out.println(String.format("[%s] Number of executed instructions: %s", ICount.class.getSimpleName(), ninsts));
        saveStatistics();
        System.out.println("/////////////////////////////////////////////////////");
    
    }

    public static void saveStatistics() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("metrics.txt", true));
            
            writer.println(String.format("[%s] Number of executed methods: %s", ICount.class.getSimpleName(), nmethods));
            writer.println(String.format("[%s] Number of executed basic blocks: %s", ICount.class.getSimpleName(), nblocks));
            writer.println(String.format("[%s] Number of executed instructions: %s", ICount.class.getSimpleName(), ninsts));
            // Use fully qualified class name here to avoid ambiguity
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            writer.println(String.format("CPU Load: %.2f", osBean.getSystemCpuLoad() * 100));

            long usedMemoryBytes = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
            double usedMemoryMB = usedMemoryBytes / 1048576.0; // Convert bytes to megabytes
            writer.println(String.format("Memory Used: %.2f MB", usedMemoryMB));
            // Disk I/O and Bandwidth still need OS-specific commands or external libraries


            clearCounters();
            
            writer.println("/////////////////////////////////////////////////////");
            writer.close();  // Make sure to close the writer to flush the data to the file
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public static void clearStatistics() {
        try {
            // Open the file in overwrite mode which will clear its contents
            new FileWriter("metrics.txt", false).close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public static void clearCounters() {
        // Reset the counters
        nblocks = 0;
        nmethods = 0;
        ninsts = 0;
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICount.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("handleRequest")) {
            behavior.insertBefore(String.format("%s.clearCounters();", ICount.class.getName()));
            behavior.insertAfter(String.format("%s.printStatistics();", ICount.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
