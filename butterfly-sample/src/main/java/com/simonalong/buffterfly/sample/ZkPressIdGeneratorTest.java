package com.simonalong.buffterfly.sample;

import com.simonalong.butterfly.worker.zookeeper.ZkButterflyConfig;
import lombok.SneakyThrows;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author shizi
 * @since 2020/5/3 2:11 AM
 */
public class ZkPressIdGeneratorTest extends BaseTest {


    @BeforeClass
    public static void beforeClass() {
        config = new ZkButterflyConfig();
        ((ZkButterflyConfig)config).setHost("localhost:2181");
    }

    /**
     * 单次测试
     */
    @Test
    public void singleTest() {
        baseSingleRun();
    }

    /**
     * 基本测试
     */
    @Test
    public void baseRunTest() {
        baseRun();
        //{symbol=0, sequence=1, workerId=12, abstractTime=2020-05-07 22:58:25.787, time=601105787, uuid=2521220406845452}
        //{symbol=0, sequence=2, workerId=12, abstractTime=2020-05-07 22:58:25.787, time=601105787, uuid=2521220406853644}
        //{symbol=0, sequence=1, workerId=7, abstractTime=2020-05-07 22:58:26.040, time=601106040, uuid=2521221468004359}
        //{symbol=0, sequence=2, workerId=7, abstractTime=2020-05-07 22:58:26.040, time=601106040, uuid=2521221468012551}
        //{symbol=0, sequence=3, workerId=7, abstractTime=2020-05-07 22:58:26.040, time=601106040, uuid=2521221468020743}
        //{symbol=0, sequence=4, workerId=7, abstractTime=2020-05-07 22:58:26.040, time=601106040, uuid=2521221468028935}
        //{symbol=0, sequence=1, workerId=4, abstractTime=2020-05-07 22:58:26.065, time=601106065, uuid=2521221572861956}
        //{symbol=0, sequence=1, workerId=8, abstractTime=2020-05-07 22:58:26.093, time=601106093, uuid=2521221690302472}
    }

    /**
     * 持续的低qps的压测，则这个QPS还是比较低，完全满足业务需求
     */
    @Test
    @SneakyThrows
    public void testQps1() {
        lowPressRun();

        //biz=biz0, qps = 2.9411764705882355单位（w/s）
        //biz=biz0, qps = 6.666666666666667单位（w/s）
        //biz=biz0, qps = 7.6923076923076925单位（w/s）
        //biz=biz0, qps = 5.0单位（w/s）
        //biz=biz0, qps = 6.666666666666667单位（w/s）
        //biz=biz0, qps = 8.333333333333334单位（w/s）
        //biz=biz0, qps = 7.6923076923076925单位（w/s）
        //biz=biz0, qps = 10.0单位（w/s）
        //biz=biz0, qps = 10.0单位（w/s）
        //biz=biz0, qps = 10.0单位（w/s）
    }

    /**
     * 低qps一段时间后到高QPS，可以支撑更高，但是一旦持续的高并发，则后面会慢慢降下来
     */
    @Test
    @SneakyThrows
    public void testQps2() {
        lowToHighPressRun();

        //biz=biz0, qps = 6.666666666666667单位（w/s）
        //biz=biz0, qps = 75.625单位（w/s）
        //biz=biz0, qps = 273.3333333333333单位（w/s）
        //biz=biz0, qps = 785.9090909090909单位（w/s）
        //biz=biz0, qps = 820.7843137254902单位（w/s）
        //biz=biz0, qps = 836.6666666666666单位（w/s）
        //biz=biz0, qps = 830.3单位（w/s）
        //biz=biz0, qps = 68.43609733049847单位（w/s）
        //biz=biz0, qps = 57.405300738230665单位（w/s）
        //biz=biz0, qps = 55.03019909029901单位（w/s）
    }

    /**
     * 持续的高QPS，则只能达到最高的理论值（51.2w/s）
     */
    @Test
    @SneakyThrows
    public void testQps3() {
        highPressRun();

        //biz=biz0, qps = 52.25752508361204单位（w/s）
        //biz=biz0, qps = 53.96945328943818单位（w/s）
        //biz=biz0, qps = 53.972366148531954单位（w/s）
        //biz=biz0, qps = 53.972366148531954单位（w/s）
        //biz=biz0, qps = 53.98110661268556单位（w/s）
        //biz=biz0, qps = 53.96945328943818单位（w/s）
        //biz=biz0, qps = 53.97527932207049单位（w/s）
        //biz=biz0, qps = 53.963628514381305单位（w/s）
        //biz=biz0, qps = 53.972366148531954单位（w/s）
        //biz=biz0, qps = 53.963628514381305单位（w/s）
    }

    /**
     * 多业务的压测：低qps一段时间后到高QPS，可以支撑更高
     */
    @Test
    @SneakyThrows
    public void testQps4() {
        lowToHighMultiBizPressRun();

        //biz=biz0, qps = 5.555555555555555单位（w/s）
        //biz=biz0, qps = 67.22222222222223单位（w/s）
        //biz=biz0, qps = 574.0单位（w/s）
        //biz=biz0, qps = 720.4166666666666单位（w/s）
        //biz=biz0, qps = 664.4444444444445单位（w/s）
        //biz=biz0, qps = 821.0280373831775单位（w/s）
        //biz=biz0, qps = 775.981308411215单位（w/s）
        //biz=biz0, qps = 74.47043701799485单位（w/s）
        //biz=biz0, qps = 59.53062248995984单位（w/s）
        //biz=biz0, qps = 57.143631436314365单位（w/s）

        //biz=biz1, qps = 20.0单位（w/s）
        //biz=biz1, qps = 134.44444444444446单位（w/s）
        //biz=biz1, qps = 574.0单位（w/s）
        //biz=biz1, qps = 960.5555555555555单位（w/s）
        //biz=biz1, qps = 930.2222222222222单位（w/s）
        //biz=biz1, qps = 844.7115384615385单位（w/s）
        //biz=biz1, qps = 1044.4025157232704单位（w/s）
        //biz=biz1, qps = 995.4982817869416单位（w/s）
        //biz=biz1, qps = 960.2024291497976单位（w/s）
        //biz=biz1, qps = 894.5575757575757单位（w/s）
    }
}
