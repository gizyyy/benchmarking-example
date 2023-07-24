package org.example.benchmark;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/*
By default JHM forks a new java process for each trial (set of iterations).
This is required to defend the test from previously collected “profiles” – information about other
loaded classes and their execution information. For example, if you have 2 classes implementing the
same interface and test the performance of both of them, then the first implementation
(in order of testing) is likely to be faster than the second one (in the same JVM),
because JIT replaces direct method calls to the first implementation with interface method calls
after discovering the second implementation.
 */

/*@Fork(value = 3, warmups = 2) means that 5 forks will be run sequentially. The first two will be warmup runs which will be ignored, and the final 3 will be used for benchmarking.
@Warmup(iterations = 5, time = 55, timeUnit = TimeUnit.MILLISECONDS) means that there will be 5 warmup iterations within each fork. The timings from these runs will be ignored when producing the benchmark results.
@Measurement(iterations = 4, time = 44, timeUnit = TimeUnit.MILLISECONDS) means that your benchmark iterations will be run 4 times (after the 5 warmup iterations).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
//how many times a benchmark will dry run before results are collected
//@Warmup(iterations = 3)
//@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
//Runs on 5 vms
@Fork(value = 2, warmups = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class IntegerListSum {

  private List<Integer> jdkIntList;
  private ExecutorService executor;

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(IntegerListSum.class.getSimpleName())
        .resultFormat(ResultFormatType.CSV)
        .build();

    new Runner(opt).run();
  }


  @Setup
  public void setup() {
    jdkIntList = new ArrayList<>();
    IntStream.range(1, 10000).forEach(i -> jdkIntList.add(i));
    executor = Executors.newWorkStealingPool();
  }

  @Benchmark
  public List<Long> jdkList() {
    return jdkIntList.stream().map(i -> i.longValue()).toList();
  }


  @Benchmark
  public List<Long> jdkListParallel() {
    return jdkIntList.parallelStream().map(i -> i.longValue()).toList();
  }

  @Benchmark
  public List<Long> jdkListFlux() {
    return Flux.fromIterable(jdkIntList)
        .map(Integer::longValue)
        .collectList()
        .block();

  }

  @Benchmark
  public List<Long> jdkListFluxParallel() {
    return Flux.fromIterable(jdkIntList)
        .parallel()
        .runOn(Schedulers.fromExecutor(executor))
        .map(Integer::longValue)
        .sequential()
        .publishOn(Schedulers.single())
        .collectList()
        .block();

  }

  //For spring applications
/*

    @Setup(Level.Trial)
    public void setupBenchmark() {
        startSpringApplication();
        injectBeans();
        initTestData();
    }

  private void startSpringApplication(){
    var app = new SpringApplication(RouterApplication.class);
    Properties properties = new Properties();
    app.setDefaultProperties(properties);
    app.setAdditionalProfiles("local");
    context = app.run();
  }

  private void injectBeans(){
    xRepository = context.getBean(x.class);
    yRepository = context.getBean(y.class);
  }
 */

}