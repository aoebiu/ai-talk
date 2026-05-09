package info.mengnan.aitalk.tool;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class ContextPool {

    private final BlockingQueue<Context> pool;

    public ContextPool(int poolSize) {
        this.pool = new ArrayBlockingQueue<>(poolSize);
    }

    /**
     * 向池中添加一个 Context
     */
    public void add(Context context) {
        pool.add(context);
    }

    /**
     * 从池中借出一个 Context（阻塞等待）
     */
    public Context borrow() throws InterruptedException {
        return pool.take();
    }

    /**
     * 归还 Context 到池中
     */
    public void returnContext(Context context) {
        try {
            pool.put(context);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭池中所有 Context
     */
    public void shutdown() {
        for (Context ctx : pool) {
            try {
                ctx.close();
            } catch (Exception e) {
                log.warn("Failed to close context", e);
            }
        }
    }
}
