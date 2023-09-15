package com.simonalong.butterfly.worker.db;

import com.simonalong.butterfly.sequence.exception.ButterflyException;
import com.simonalong.butterfly.sequence.exception.WorkerIdFullException;
import com.simonalong.butterfly.sequence.spi.WorkerIdHandler;
import com.simonalong.butterfly.worker.db.entity.UuidGeneratorDO;
import com.simonalong.neo.Neo;
import com.simonalong.neo.NeoMap;
import com.simonalong.neo.Pair;
import com.simonalong.neo.TableMap;
import com.simonalong.neo.util.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.simonalong.butterfly.sequence.UuidConstant.*;
import static com.simonalong.butterfly.worker.db.DbConstant.DB_LOG_PRE;
import static com.simonalong.butterfly.worker.db.DbConstant.UUID_TABLE;

/**
 * @author shizi
 * @since 2020/4/25 12:51 AM
 */
@Slf4j
public class DbWorkerIdHandler implements WorkerIdHandler {

    /**
     * 本次启动的唯一key
     */
    private String uidKey;
    /**
     * 进程id
     */
    private String processId;
    private String ip;
    /**
     * worker_x 节点信息
     */
    private ScheduledThreadPoolExecutor scheduler;
    private DbOrmProxy ormProxy;
    private String namespace;
    private UuidGeneratorDO uuidGeneratorDO;

    public DbWorkerIdHandler(String namespace, DbOrmProxy ormProxy) {
        this.namespace = namespace;
        this.ormProxy = ormProxy;
        this.uuidGeneratorDO = new UuidGeneratorDO();
        init();
    }

    private void init() {
        checkNamespace(namespace);

        // 配置基本信息
        initBaseInfo();

        // 分配worker
        allocateWorker();

        // 初始化心跳上报
        initHeartBeatReport();

        // 添加进程退出钩子
        addShutdownHook();
    }

    @Override
    public Long getLastExpireTime() {
        return LocalDateTimeUtil.timestampToLong(uuidGeneratorDO.getLastExpireTime());
    }

    @Override
    public Integer getWorkerId() {
        return uuidGeneratorDO.getWorkId();
    }

    private void initBaseInfo() {
        uidKey = UUID.randomUUID().toString();
        processId = getProcessIdStr();
        ip = getIpStr();
    }

    /**
     * 初始化数据的心跳上报
     */
    private void initHeartBeatReport() {
        scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private final AtomicInteger threadNum = new AtomicInteger(0);

            @Override
            @SuppressWarnings("all")
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Neo-Heart-Thread-" + threadNum.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });

        // 延迟10秒上报，每5秒上报一次数据
        scheduler.scheduleWithFixedDelay(this::refreshNodeInfo, 10, HEART_TIME, TimeUnit.SECONDS);
    }

    /**
     * 进程关闭时候清理业务数据
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info(DB_LOG_PRE + "process ready to quit, clear resources of db");
            ormProxy.delete(uuidGeneratorDO.getId());
            if (null != scheduler) {
                scheduler.shutdown();
            }
        }));
    }

    /**
     * 刷新节点信息
     * <p>
     * 主要为更新下次失效时间
     */
    private void refreshNodeInfo() {
        try {
            long lastExpireTime = afterHour();
            UuidGeneratorDO newGenerate = new UuidGeneratorDO();
            newGenerate.setId(uuidGeneratorDO.getId());
            newGenerate.setLastExpireTime(LocalDateTimeUtil.longToTimestamp(lastExpireTime));
            newGenerate = ormProxy.update(UUID_TABLE, newGenerate);
            if (null != newGenerate && null != newGenerate.getId()) {
                uuidGeneratorDO.setLastExpireTime(LocalDateTimeUtil.longToTimestamp(lastExpireTime));
            }
        } catch (Throwable e) {
            log.error("刷新节点信息异常：", e);
        }
    }

    private void allocateWorker() {
        if (applyWorkerFromExistExpire()) {
            return;
        }

        //新增一个worker
        uuidGeneratorDO = ormProxy.insertWorker(namespace, uidKey, processId, ip);
    }

    private void checkNamespace(String namespace) {
        if (null == namespace || "".equals(namespace)) {
            throw new ButterflyException("命名空间为空");
        }

        if (namespace.equals(DISTRIBUTE_SERVER)) {
            throw new ButterflyException("node [" + namespace + "] is distribute'node, forbidden to add");
        }
    }

    /**
     * 从已存在的worker里面查看过期的，有过期的则获取并更新数据
     *
     * @return true：分配成功，false：分配失败
     */
    private Boolean applyWorkerFromExistExpire() {
        Long minId = ormProxy.selectMinId(namespace);
        if (null == minId) {
            return false;
        }

        Pair<Boolean, UuidGeneratorDO> pair = ormProxy.doApplyWorkerFromExistExpire(minId, namespace, uidKey, processId, ip);
        if (pair.getKey()) {
            uuidGeneratorDO = pair.getValue();
            return true;
        }
        return false;
    }

    private String getIpStr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * 将时间向未来延长固定的小时
     */
    private long afterHour() {
        return System.currentTimeMillis() + KEEP_NODE_EXIST_TIME;
    }

    /**
     * 获取进程id字符串
     */
    private String getProcessIdStr() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }
}
