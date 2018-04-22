package com.codegans.jmh.vh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JavaDoc here
 *
 * @author Victor Polischuk
 * @since 22.04.2018 18:21
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VarHandleBenchmark {
    @State(Scope.Benchmark)
    public static class Handles {
        private final Field field;
        private final VarHandle varHandle;
        private final AtomicLong success;
        private final AtomicLong fail;

        public Handles() {
            try {
                this.field = Model.class.getDeclaredField("data");
                this.varHandle = MethodHandles.lookup().findVarHandle(Model.class, "data", byte[].class);
                this.success = new AtomicLong(0);
                this.fail = new AtomicLong(0);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        @TearDown(Level.Trial)
        public void stats() throws InterruptedException {
            System.out.print("<<<Passed: " + success.get() + ". Failed: " + fail.get() + ". Total: " + (success.get() + fail.get()) + ">>>");
        }

        byte[] executeN(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (model.data == oldValue) {
                model.data = newValue;
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }

        synchronized byte[] executeSN(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (model.data == oldValue) {
                model.data = newValue;
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }

        byte[] executeVH(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (varHandle.get(model) == oldValue) {
                varHandle.set(model, newValue);
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }

        byte[] executeSVH(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (varHandle.compareAndSet(model, oldValue, newValue)) {
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }

        byte[] executeF(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (field.get(model) == oldValue) {
                field.set(model, newValue);
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }

        synchronized byte[] executeSF(Model model, byte[] oldValue, byte[] newValue) throws IllegalAccessException {
            if (field.get(model) == oldValue) {
                field.set(model, newValue);
                success.incrementAndGet();
                return newValue;
            } else {
                fail.incrementAndGet();
            }

            return oldValue;
        }
    }

    @State(Scope.Benchmark)
    public static class Model {
        private static final byte[][] PREPARED = new byte[3][100_000];

        volatile byte[] data;

        void test(long i, Model model) {
            
        }

        @Setup
        public void setup() {
            for (int i = 0; i < 3; i++) {
                data = new byte[PREPARED[i].length];

                Arrays.fill(data, (byte) ('A' + i));

                PREPARED[i] = data;
            }
        }
    }

    @Benchmark
    public void testNative(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeN(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    @Benchmark
    public void testSafeNative(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeSN(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    @Benchmark
    public void testVarHandle(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeVH(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    @Benchmark
    public void testSafeVarHandle(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeSVH(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    @Benchmark
    public void testField(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeF(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    @Benchmark
    public void testSafeField(Model model, Handles handles, Blackhole hole) throws IllegalAccessException {
        hole.consume(handles.executeSF(model, model.data, Model.PREPARED[handles.success.intValue() % 3]));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VarHandleBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(6)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
