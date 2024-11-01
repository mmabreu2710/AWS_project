package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.HashMap;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class BasicBlockCache extends AbstractJavassistTool {

    /**
     * Number of executed basic blocks.
     */
    private static long nblocks = 0;

    /**
     * Number of executed instructions.
     */
    private static long ninsts = 0;

    private static int cache_hits = 0;
    private static int cache_misses = 0;
    private static int cache_size = 128;

    private static HashMap<Integer, BasicBlock> map;

    public BasicBlockCache(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
        map = new HashMap<>();
    }

    public static void cacheCount(int position, int length) {
        nblocks++;
        ninsts += length;
        System.out.println("position: " + position + "; length: " + length);
        if(map.containsKey(position)) {
            cache_hits++;
        }
        else {
            map.put(position, null);
            cache_misses++;
        }
    }

    public static void printStatistics() {
        System.out.println(String.format("[%s] Number of executed basic blocks: %s", BasicBlockCache.class.getSimpleName(), nblocks));
        System.out.println(String.format("[%s] Number of executed instructions: %s", BasicBlockCache.class.getSimpleName(), ninsts));
        System.out.println(String.format("[%s] Cache hits: %s", BasicBlockCache.class.getSimpleName(), cache_hits));
        System.out.println(String.format("[%s] Cache misses: %s", BasicBlockCache.class.getSimpleName(), cache_misses));


    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", BasicBlockCache.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.cacheCount(%s, %s);", BasicBlockCache.class.getName(), block.getPosition(), block.getLength()));
    }

}

