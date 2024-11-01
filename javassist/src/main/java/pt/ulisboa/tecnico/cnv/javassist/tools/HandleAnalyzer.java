package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.io.FileWriter;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

import java.io.PrintWriter;
import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;


public class HandleAnalyzer extends AbstractJavassistTool {

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

    private static long opTime = 0;

    private static String ipAddress = "127.0.0.1";

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static {
        // Start the periodic task as soon as the class is loaded
        scheduler.scheduleAtFixedRate(() -> {
            try {
                printAndWriteVMState();
            } catch (Exception e) {
                System.err.println("Error during scheduled execution of printAndWriteVMState: " + e.getMessage());
            }
        }, 3, 5, TimeUnit.SECONDS);

        // Shutdown hook to clean up when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownScheduler();
        }));
    }

    public HandleAnalyzer(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }
    

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void getOpTime(long op) {
        opTime = op;
    }
    public static boolean isVMHealthy() {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            // Attempt to ping the LoadBalancer; timeout is 5000 milliseconds (5 seconds)
            return address.isReachable(5000);
        } catch (Exception e) {
            System.out.println("Error checking VM health: " + e.getMessage());
            return false;
        }
    }
    
    public static void printAndWriteVMState() {
        // Use fully qualified class name here to avoid ambiguity
        try{
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            long usedMemoryBytes = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
            double usedMemoryMB = usedMemoryBytes / 1048576.0;
            boolean VMHealthy = isVMHealthy();
            long threadId = Thread.currentThread().getId();
            //int port = getServerPort(); // Retrieve the stored port number

            /*System.out.println(String.format("[%s] Running on IP: %s ", HandleAnalyzer.class.getSimpleName(), ipAddress));
        
            System.out.println(String.format("[%s] CPU Load: %.2f%%", HandleAnalyzer.class.getSimpleName(), cpuLoad));
            
            System.out.println(String.format("[%s] Memory Used: %.2f MB", HandleAnalyzer.class.getSimpleName(), usedMemoryMB));

            System.out.println(String.format("[%s] VM Health: %s", HandleAnalyzer.class.getSimpleName(), VMHealthy ? "Yes" : "No"));

            System.out.println(String.format("[%s] Thread ID: %d)", HandleAnalyzer.class.getSimpleName(), threadId));

            // Disk I/O and Bandwidth still need OS-specific commands or external libraries
            
            System.out.println("/////////////////////////////////////////////////////");*/

             // Execute the shell script
            vmState_DBShellScript(cpuLoad, usedMemoryMB, VMHealthy, threadId);
            

        } catch (IOException e) {
            System.err.println("Error obtaining local IP address: " + e.getMessage());
        }
    }

    private static void vmState_DBShellScript(double cpuLoad, double memoryUsed, boolean vmHealthy, long threadId) {
        String scriptPath = "./run_VMState.sh";
        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", scriptPath,
                String.valueOf(cpuLoad),
                String.valueOf(memoryUsed),
                String.valueOf(vmHealthy),
                ipAddress,
                String.valueOf(threadId)
        );

        try {
            // Run the command
            Process process = processBuilder.start();

            // Optionally, read the output of the command
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Wait for the process to complete and get the exit value
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.err.println("Command execution failed with exit code " + exitValue);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing shell script: " + e.getMessage());
        }
    }
        

    public static void transformRaytracer(byte[] input, byte[] texmap, int scols, int srows, int wcols, int wrows, int coff, int roff) {
        try {
            System.out.println("TransformRaytracer started");
            System.out.println(String.format("[%s] Number of executed methods: %s", HandleAnalyzer.class.getSimpleName(), nmethods));
            System.out.println(String.format("[%s] Number of executed basic blocks: %s", HandleAnalyzer.class.getSimpleName(), nblocks));
            System.out.println(String.format("[%s] Number of executed instructions: %s", HandleAnalyzer.class.getSimpleName(), ninsts));
            System.out.println(String.format("[%s] Completed in: %s ns", HandleAnalyzer.class.getSimpleName(), opTime));
    
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            long usedMemoryBytes = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
            double usedMemoryMB = usedMemoryBytes / 1048576.0;
            boolean VMHealthy = isVMHealthy();
            long threadId = Thread.currentThread().getId();
    
            System.out.println(String.format("[%s] Running on IP: %s ", HandleAnalyzer.class.getSimpleName(), ipAddress));
            System.out.println(String.format("[%s] CPU Load: %.2f%%", HandleAnalyzer.class.getSimpleName(), cpuLoad));
            System.out.println(String.format("[%s] Memory Used: %.2f MB", HandleAnalyzer.class.getSimpleName(), usedMemoryMB));
            System.out.println(String.format("[%s] VM Health: %s", HandleAnalyzer.class.getSimpleName(), VMHealthy ? "Yes" : "No"));
            System.out.println(String.format("[%s] Thread ID: %d)", HandleAnalyzer.class.getSimpleName(), threadId));
    
            System.out.println("Hashing the byte arrays");

            int SizeOfInput = input.length;

            // Check for null values before hashing
            String inputHash = input != null ? hashByteArray(input) : "null";
            String texmapHash = texmap != null ? hashByteArray(texmap) : "null";

            double score = calculateScoreOfRaytracer(nmethods, nblocks, ninsts, opTime, cpuLoad, usedMemoryMB, SizeOfInput);
    
            System.out.println("Running raytracer_DBShellScript");
            raytracer_DBShellScript(inputHash, texmapHash, scols, srows, wcols, wrows, coff, roff, cpuLoad, usedMemoryMB, VMHealthy, threadId, SizeOfInput, score);
    
            System.out.println("Clearing counters");
            clearCounters();
            System.out.println("TransformRaytracer finished");
        } catch (Exception e) {
            System.err.println("Exception in transformRaytracer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void raytracer_DBShellScript(String input, String texmap, int scols, int srows, int wcols, int wrows, int coff, int roff, double cpuLoad, double memoryUsed, boolean vmHealthy, long threadId, int SizeOfInput, double score) {
        String scriptPath = "./run_rayTracer_DB.sh";
        System.out.println("Preparing ProcessBuilder with script: " + scriptPath);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", scriptPath,
                input,
                texmap,
                String.valueOf(scols),
                String.valueOf(srows),
                String.valueOf(wcols),
                String.valueOf(wrows),
                String.valueOf(coff),
                String.valueOf(roff),
                String.valueOf(nmethods),
                String.valueOf(nblocks),
                String.valueOf(ninsts),
                String.valueOf(opTime),
                String.valueOf(cpuLoad),
                String.valueOf(memoryUsed),
                String.valueOf(vmHealthy),
                ipAddress,
                String.valueOf(threadId),
                String.valueOf(SizeOfInput),
                String.valueOf(score)
        );
    
        try {
            System.out.println("Starting the process");
            Process process = processBuilder.start();
    
            System.out.println("Reading process output");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
    
            System.out.println("Reading process error stream");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            }
    
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.err.println("Command execution failed with exit code " + exitValue);
            } else {
                System.out.println("Command executed successfully with exit code " + exitValue);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exception while executing shell script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static double calculateScoreOfRaytracer(long nmethods, long nblocks, long ninsts, long opTime, double cpuLoad, double usedMemoryMB, int sizeOfInput) {
        // Define max values for normalization (based on expected ranges or empirical data)
        double maxMethods = 30000000.0;
        double maxBlocks = 50000000.0;
        double maxInsts = 1400000000.0;
        double maxOpTime = 10000000000.0; // 10s
        double maxCpuLoad = 100.0;
        double maxMemoryMB = 1024.0; // 1 GB
        double maxSizeOfInput = 10000.0; // 1 KB

        // Normalize metrics
        double normMethods = Math.min(nmethods / maxMethods, 1.0);
        double normBlocks = Math.min(nblocks / maxBlocks, 1.0);
        double normInsts = Math.min(ninsts / maxInsts, 1.0);
        double normOpTime = Math.min(opTime / maxOpTime, 1.0);
        double normCpuLoad = Math.min(cpuLoad / maxCpuLoad, 1.0);
        double normMemoryMB = Math.min(usedMemoryMB / maxMemoryMB, 1.0);
        double normSizeOfInput = Math.min(sizeOfInput / maxSizeOfInput, 1.0);

        // Define weights
        double weightMethods = 0.1;
        double weightBlocks = 0.1;
        double weightInsts = 0.05;
        double weightOpTime = 0.25;
        double weightCpuLoad = 0.25;
        double weightMemoryMB = 0.2;
        double weightSizeOfInput = 0.05;

        // Calculate weighted score
        double score = (normMethods * weightMethods +
                        normBlocks * weightBlocks +
                        normInsts * weightInsts +
                        normOpTime * weightOpTime +
                        normCpuLoad * weightCpuLoad +
                        normMemoryMB * weightMemoryMB +
                        normSizeOfInput * weightSizeOfInput) * 100;

        return score;
    }

    private static String hashByteArray(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }


    public static void transformImageProcessing(String inputEncoded, String format, String requestedURI) {
        try{
            System.out.println(String.format("[%s] Number of executed methods: %s", HandleAnalyzer.class.getSimpleName(), nmethods));
            System.out.println(String.format("[%s] Number of executed basic blocks: %s", HandleAnalyzer.class.getSimpleName(), nblocks));
            System.out.println(String.format("[%s] Number of executed instructions: %s", HandleAnalyzer.class.getSimpleName(), ninsts));
            System.out.println(String.format("[%s] Completed in: %s ns", HandleAnalyzer.class.getSimpleName(), opTime));

            ipAddress = InetAddress.getLocalHost().getHostAddress();
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            long usedMemoryBytes = osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize();
            double usedMemoryMB = usedMemoryBytes / 1048576.0;
            boolean VMHealthy = isVMHealthy();
            long threadId = Thread.currentThread().getId();
            //int port = getServerPort(); // Retrieve the stored port number

            System.out.println(String.format("[%s] Running on IP: %s ", HandleAnalyzer.class.getSimpleName(), ipAddress));
        
            System.out.println(String.format("[%s] CPU Load: %.2f%%", HandleAnalyzer.class.getSimpleName(), cpuLoad));
            
            System.out.println(String.format("[%s] Memory Used: %.2f MB", HandleAnalyzer.class.getSimpleName(), usedMemoryMB));

            System.out.println(String.format("[%s] VM Health: %s", HandleAnalyzer.class.getSimpleName(), VMHealthy ? "Yes" : "No"));

            System.out.println(String.format("[%s] Thread ID: %d)", HandleAnalyzer.class.getSimpleName(), threadId));
            // Disk I/O and Bandwidth still need OS-specific commands or external libraries
            
            System.out.println("/////////////////////////////////////////////////////");

            int SizeOfInput = getStringSizeInBytes(inputEncoded);

            String inputHash = inputEncoded != null ? hashString(inputEncoded) : "null";

            double score = calculateScoreOfImageProc(nmethods, nblocks, ninsts, opTime, cpuLoad, usedMemoryMB, SizeOfInput);

            imageproc_DBShellScript(inputHash, format, cpuLoad, usedMemoryMB, VMHealthy, threadId, requestedURI, SizeOfInput, score);
            clearCounters();
        } catch (UnknownHostException e) {
            System.err.println("Error obtaining local IP address: " + e.getMessage());
        }
    }
    private static void imageproc_DBShellScript(String inputHash, String format, double cpuLoad, double memoryUsed, boolean vmHealthy, long threadId, String requestedURI, int SizeOfInput, double score) {
        String scriptPath = "./run_imageProc_DB.sh";
        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash", scriptPath,
                String.valueOf(inputHash),
                String.valueOf(format),
                String.valueOf(nmethods),
                String.valueOf(nblocks),
                String.valueOf(ninsts),
                String.valueOf(opTime),
                String.valueOf(cpuLoad),
                String.valueOf(memoryUsed),
                String.valueOf(vmHealthy),
                ipAddress,
                String.valueOf(threadId),
                requestedURI,
                String.valueOf(SizeOfInput),
                String.valueOf(score)
        );

        try {
            // Run the command
            Process process = processBuilder.start();

            // Optionally, read the output of the command
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }


            // Wait for the process to complete and get the exit value
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.err.println("Command execution failed with exit code " + exitValue);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing shell script: " + e.getMessage());
        }
    }

    private static String hashString(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static int getStringSizeInBytes(String str) {
        try {
            byte[] byteArray = str.getBytes("UTF-8");
            return byteArray.length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding" , e);
        }
    }

    public static double calculateScoreOfImageProc(long nmethods, long nblocks, long ninsts, long opTime, double cpuLoad, double usedMemoryMB, int sizeOfInput) {
        // Define max values for normalization (based on expected ranges or empirical data)
        double maxMethods = 5.0;
        double maxBlocks = 10.0;
        double maxInsts = 250.0;
        double maxOpTime = 5000000000.0; // 5s
        double maxCpuLoad = 100.0;
        double maxMemoryMB = 1024.0; // 1 GB
        double maxSizeOfInput = 2500000.0; // 250 KB

        // Normalize metrics
        double normMethods = Math.min(nmethods / maxMethods, 1.0);
        double normBlocks = Math.min(nblocks / maxBlocks, 1.0);
        double normInsts = Math.min(ninsts / maxInsts, 1.0);
        double normOpTime = Math.min(opTime / maxOpTime, 1.0);
        double normCpuLoad = Math.min(cpuLoad / maxCpuLoad, 1.0);
        double normMemoryMB = Math.min(usedMemoryMB / maxMemoryMB, 1.0);
        double normSizeOfInput = Math.min(sizeOfInput / maxSizeOfInput, 1.0);

        // Define weights
        double weightMethods = 0.1;
        double weightBlocks = 0.1;
        double weightInsts = 0.05;
        double weightOpTime = 0.25;
        double weightCpuLoad = 0.25;
        double weightMemoryMB = 0.2;
        double weightSizeOfInput = 0.05;

        // Calculate weighted score
        double score = (normMethods * weightMethods +
                        normBlocks * weightBlocks +
                        normInsts * weightInsts +
                        normOpTime * weightOpTime +
                        normCpuLoad * weightCpuLoad +
                        normMemoryMB * weightMemoryMB +
                        normSizeOfInput * weightSizeOfInput) * 100;

        return score;
    }
    

    public static void clearCounters() {
        // Reset the counters
        nblocks = 0;
        nmethods = 0;
        ninsts = 0;
    }

    // Helper method to cleanly shutdown the scheduler
    private static void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", HandleAnalyzer.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("handleRequest")) {
            behavior.addLocalVariable("startTime", CtClass.longType);
            behavior.insertBefore("startTime = System.nanoTime();");
            StringBuilder builder = new StringBuilder();
            behavior.addLocalVariable("endTime", CtClass.longType);
            behavior.addLocalVariable("opTime", CtClass.longType);
            builder.append("endTime = System.nanoTime();");
            builder.append("opTime = endTime-startTime;");
            builder.append(String.format("%s.getOpTime(opTime);", HandleAnalyzer.class.getName()));
            behavior.insertAfter(builder.toString());

            behavior.insertBefore(String.format("%s.clearCounters();", HandleAnalyzer.class.getName()));
            //behavior.insertAfter(String.format("%s.printStatistics();", HandleAnalyzer.class.getName()));

            // Get the number of parameters in the method
            int paramCount = behavior.getParameterTypes().length;

            // Additional code to handle specific arguments for RaytracerHandler and ImageProcessingHandler
            if (behavior.getDeclaringClass().getName().equals("pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler") && paramCount == 8) {
                System.out.println("Entrou Raytracer");
                System.out.println("/////////////////////////////");
                behavior.insertAfter(String.format("%s.transformRaytracer($1, $2, $3, $4, $5, $6, $7, $8);", HandleAnalyzer.class.getName()));
            } else if (behavior.getDeclaringClass().getName().equals("pt.ulisboa.tecnico.cnv.imageproc.ImageProcessingHandler") && paramCount == 3) {
                System.out.println("Entrou ImageProc");
                System.out.println("/////////////////////////////");
                behavior.insertAfter(String.format("%s.transformImageProcessing((String)$1, (String)$2, (String)$3);", HandleAnalyzer.class.getName()));
            }
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", HandleAnalyzer.class.getName(), block.getPosition(), block.getLength()));
    }

}
